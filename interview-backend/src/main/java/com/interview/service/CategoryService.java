package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.entity.Category;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final String CACHE_KEY = "cache:category:tree";

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ContentCacheService contentCacheService;
    private final AdminLogService adminLogService;

    public List<VOs.CategoryVO> tree() {
        String cached = redis.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<VOs.CategoryVO>>() {
                });
            } catch (JsonProcessingException ignored) {
            }
        }
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByDesc(Category::getPriority)
                        .orderByAsc(Category::getName)
                        .orderByAsc(Category::getId));
        List<VOs.CategoryVO> tree = buildTree(all, 0L);
        try {
            redis.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(tree), Duration.ofMinutes(30));
        } catch (JsonProcessingException ignored) {
        }
        return tree;
    }

    public List<Long> collectIds(Long categoryId) {
        List<Category> all = categoryMapper.selectList(null);
        Map<Long, List<Long>> children = new LinkedHashMap<>();
        for (Category c : all) {
            children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
        }
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
                vo.setPriority(c.getPriority());
                vo.setDescription(c.getDescription());
                vo.setChildren(buildTree(all, c.getId()));
                result.add(vo);
            }
        }
        return result;
    }

    public void clearCache() {
        redis.delete(CACHE_KEY);
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
        category.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        category.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        categoryMapper.insert(category);
        clearCache();
        contentCacheService.bump();
        adminLogService.write("CATEGORY_CREATE", "CATEGORY", category.getId(), "新增分类 " + category.getName());
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
        category.setPriority(dto.getPriority() == null ? 0 : dto.getPriority());
        category.setDescription(dto.getDescription() == null ? "" : dto.getDescription());
        categoryMapper.updateById(category);
        clearCache();
        contentCacheService.bump();
        adminLogService.write("CATEGORY_UPDATE", "CATEGORY", category.getId(), "编辑分类 " + category.getName());
        return toVO(category);
    }

    private VOs.CategoryVO toVO(Category category) {
        VOs.CategoryVO vo = new VOs.CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setSlug(category.getSlug());
        vo.setParentId(category.getParentId());
        vo.setSortOrder(category.getSortOrder());
        vo.setPriority(category.getPriority());
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
        adminLogService.write("CATEGORY_DELETE", "CATEGORY", id, "删除分类");
    }
}
