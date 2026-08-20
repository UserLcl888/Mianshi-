package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.RedisKeys;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.entity.Category;
import com.interview.enums.AdminLogAction;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ContentCacheService contentCacheService;
    private final AdminLogService adminLogService;

    public List<VOs.CategoryVO> tree() {
        String cached = redis.opsForValue().get(RedisKeys.CACHE_CATEGORY_TREE);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<VOs.CategoryVO>>() {
                });
            } catch (JsonProcessingException ignored) {
            }
        }
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId));
        List<VOs.CategoryVO> tree = buildTree(all, 0L);
        try {
            redis.opsForValue().set(RedisKeys.CACHE_CATEGORY_TREE, objectMapper.writeValueAsString(tree), Duration.ofMinutes(30));
        } catch (JsonProcessingException ignored) {
        }
        return tree;
    }

    public List<Long> collectIds(Long categoryId) {
        Map<Long, List<Long>> children = new LinkedHashMap<>();
        flattenChildren(tree(), children);
        List<Long> result = new ArrayList<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(categoryId);
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            result.add(id);
            List<Long> subs = children.get(id);
            if (subs != null) {
                subs.forEach(stack::push);
            }
        }
        return result;
    }

    private void flattenChildren(List<VOs.CategoryVO> nodes, Map<Long, List<Long>> children) {
        for (VOs.CategoryVO node : nodes) {
            children.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node.getId());
            flattenChildren(node.getChildren(), children);
        }
    }

    private List<VOs.CategoryVO> buildTree(List<Category> all, Long parentId) {
        List<VOs.CategoryVO> result = new ArrayList<>();
        for (Category c : all) {
            if (parentId.equals(c.getParentId())) {
                VOs.CategoryVO vo = new VOs.CategoryVO();
                vo.setId(c.getId());
                vo.setName(c.getName());
                vo.setSlug(c.getSlug());
                vo.setParentId(c.getParentId());
                vo.setSortOrder(c.getSortOrder());
                vo.setDescription(c.getDescription());
                vo.setChildren(buildTree(all, c.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    public void clearCache() {
        redis.delete(RedisKeys.CACHE_CATEGORY_TREE);
    }

    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    public VOs.CategoryVO create(Requests.CategorySaveDTO dto) {
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getSlug, dto.getSlug())) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "slug 已存在");
        }
        Category category = new Category();
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        categoryMapper.insert(category);
        clearCache();
        contentCacheService.bump();
        adminLogService.write(AdminLogAction.CATEGORY_CREATE, category.getId(), "新增分类 " + category.getName());
        return toVO(category);
    }

    public VOs.CategoryVO update(Long id, Requests.CategorySaveDTO dto) {
        Category category = getById(id);
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getSlug, dto.getSlug()).ne(Category::getId, id)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "slug 已存在");
        }
        category.setName(dto.getName());
        category.setSlug(dto.getSlug());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        categoryMapper.updateById(category);
        clearCache();
        contentCacheService.bump();
        adminLogService.write(AdminLogAction.CATEGORY_UPDATE, category.getId(), "编辑分类 " + category.getName());
        return toVO(category);
    }

    /**
     * 批量调整分类顺序（支持同一父级内重排，也支持传入新 parentId 跨级移动）。
     */
    @org.springframework.transaction.annotation.Transactional
    public void reorder(List<Requests.CategoryReorderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (Requests.CategoryReorderItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            Category category = categoryMapper.selectById(item.getId());
            if (category == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "分类不存在：" + item.getId());
            }
            if (item.getParentId() != null && !item.getParentId().equals(category.getParentId())) {
                validateNewParent(item.getId(), item.getParentId());
                category.setParentId(item.getParentId());
            }
            category.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            categoryMapper.updateById(category);
        }
        clearCache();
        contentCacheService.bump();
    }

    private void validateNewParent(Long categoryId, Long newParentId) {
        if (categoryId.equals(newParentId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能移动到自身下");
        }
        List<Category> all = categoryMapper.selectList(null);
        Map<Long, List<Long>> children = new HashMap<>();
        for (Category c : all) {
            children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
        }
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(categoryId);
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            if (id.equals(newParentId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "不能移动到自己的子分类下");
            }
            List<Long> subs = children.get(id);
            if (subs != null) {
                subs.forEach(stack::push);
            }
        }
    }

    private VOs.CategoryVO toVO(Category category) {
        VOs.CategoryVO vo = new VOs.CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSlug(category.getSlug());
        vo.setParentId(category.getParentId());
        vo.setSortOrder(category.getSortOrder());
        vo.setDescription(category.getDescription());
        return vo;
    }

    public void delete(Long id) {
        if (categoryMapper.selectCount(new LambdaQueryWrapper<Category>().eq(Category::getParentId, id)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该分类下存在子分类，无法删除");
        }
        if (articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getCategoryId, id)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "该分类下存在题目，无法删除");
        }
        categoryMapper.deleteById(id);
        clearCache();
        contentCacheService.bump();
        adminLogService.write(AdminLogAction.CATEGORY_DELETE, id, "删除分类");
    }
}
