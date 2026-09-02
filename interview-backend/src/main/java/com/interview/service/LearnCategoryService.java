package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.entity.LearnCategory;
import com.interview.enums.AdminLogAction;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.LearnCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LearnCategoryService {

    private final LearnCategoryMapper learnCategoryMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final AdminLogService adminLogService;
    private final MarkdownImageService markdownImageService;

    public List<VOs.LearnCategoryVO> list() {
        return articleService.learnCategories();
    }

    @Transactional
    public VOs.LearnCategoryVO create(Requests.LearnCategorySaveDTO dto) {
        String name = dto.getName().trim();
        String slug = StringUtils.hasText(dto.getSlug())
                ? dto.getSlug().trim()
                : "learn-" + System.currentTimeMillis();
        if (learnCategoryMapper.selectCount(new LambdaQueryWrapper<LearnCategory>()
                .eq(LearnCategory::getSlug, slug)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "分类标识已存在");
        }
        LearnCategory category = new LearnCategory();
        category.setName(name);
        category.setSlug(slug);
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setCoverUrl(dto.getCoverUrl());
        learnCategoryMapper.insert(category);
        adminLogService.write(AdminLogAction.CATEGORY_CREATE, category.getId(), "新增学习分类：" + name);
        return toVO(category);
    }

    @Transactional
    public VOs.LearnCategoryVO update(Long id, Requests.LearnCategorySaveDTO dto) {
        LearnCategory category = get(id);
        String oldCover = category.getCoverUrl();
        category.setName(dto.getName().trim());
        if (StringUtils.hasText(dto.getSlug())) {
            String slug = dto.getSlug().trim();
            if (!slug.equals(category.getSlug())
                    && learnCategoryMapper.selectCount(new LambdaQueryWrapper<LearnCategory>()
                    .eq(LearnCategory::getSlug, slug).ne(LearnCategory::getId, id)) > 0) {
                throw new BizException(ErrorCode.CONFLICT, "分类标识已存在");
            }
            category.setSlug(slug);
        }
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        category.setCoverUrl(dto.getCoverUrl());
        learnCategoryMapper.updateById(category);
        // 替换封面：保存成功后删除旧图
        if (StringUtils.hasText(oldCover) && !oldCover.equals(category.getCoverUrl())) {
            markdownImageService.removeObjectByUrl(oldCover);
        }
        adminLogService.write(AdminLogAction.CATEGORY_UPDATE, id, "编辑学习分类：" + category.getName());
        return toVO(category);
    }

    @Transactional
    public void delete(Long id) {
        LearnCategory category = get(id);
        long used = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getLearnCategoryId, id));
        if (used > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该分类下还有学习文章，请先迁移或删除文章");
        }
        learnCategoryMapper.deleteById(id);
        adminLogService.write(AdminLogAction.CATEGORY_DELETE, id, "删除学习分类：" + category.getName());
    }

    /** 管理员拖拽调整学习专题排序（sort_order 越小越靠前）。 */
    @Transactional
    public void reorder(List<Requests.LearnCategoryReorderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (Requests.LearnCategoryReorderItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            LearnCategory category = get(item.getId());
            category.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            learnCategoryMapper.updateById(category);
        }
        adminLogService.write(AdminLogAction.CATEGORY_UPDATE, 0L, "调整学习专题排序");
    }

    private LearnCategory get(Long id) {
        LearnCategory category = learnCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学习分类不存在");
        }
        return category;
    }

    private VOs.LearnCategoryVO toVO(LearnCategory category) {
        return VOs.LearnCategoryVO.builder()
                .id(category.getId())
                .slug(category.getSlug())
                .name(category.getName())
                .coverUrl(category.getCoverUrl())
                .articleCount(0L)
                .build();
    }
}
