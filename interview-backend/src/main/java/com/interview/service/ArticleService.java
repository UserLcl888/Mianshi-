package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.PageResult;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.entity.Category;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.ArticleRelatedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleRelatedMapper articleRelatedMapper;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final MarkdownService markdownService;
    private final StringRedisTemplate redis;

    public PageResult<VOs.ArticleListItemVO> list(Long categoryId, String difficulty, long page, long size) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getStatus, 1);
        if (StringUtils.hasText(difficulty)) {
            qw.eq(Article::getDifficulty, normalizeDifficulty(difficulty));
        }
        if (categoryId != null) {
            qw.in(Article::getCategoryId, categoryService.collectIds(categoryId));
        }
        qw.orderByAsc(Article::getId);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), qw);
        List<VOs.ArticleListItemVO> list = result.getRecords().stream().map(this::toListItem).toList();
        return PageResult.of(page, size, result.getTotal(), list);
    }

    public VOs.DetailRespVO detail(String slug) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, slug).eq(Article::getStatus, 1));
        if (article == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或已下架");
        }
        Category category = categoryService.getById(article.getCategoryId());
        VOs.ArticleDetailVO detailVO = toDetail(article, category);

        VOs.DetailRespVO resp = new VOs.DetailRespVO();
        resp.setArticle(detailVO);
        List<Long> explicitIds = articleRelatedMapper.selectRelatedIdsByArticleId(article.getId());
        resp.setRelated(related(article.getId(), article.getCategoryId(), explicitIds));
        resp.setRelatedIds(explicitIds);
        resp.setPrev(neighbor(article.getId(), article.getCategoryId(), false));
        resp.setNext(neighbor(article.getId(), article.getCategoryId(), true));
        return resp;
    }

    /**
     * 浏览上报：同一用户 24 小时内对同一题目只计一次浏览。
     * 由前端在停留超过 30 秒后调用。
     */
    public Long recordView(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或已下架");
        }
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        String key = "view:user:" + userId + ":article:" + articleId;
        Boolean first = redis.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
        if (Boolean.TRUE.equals(first)) {
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId)
                    .setSql("view_count = view_count + 1"));
            return article.getViewCount() + 1;
        }
        return article.getViewCount();
    }

    private List<VOs.ArticleListItemVO> related(Long id, Long categoryId, List<Long> explicitIds) {
        List<VOs.ArticleListItemVO> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        if (explicitIds != null) {
            for (Long rid : explicitIds) {
                if (rid == null || rid.equals(id)) {
                    continue;
                }
                Article a = articleMapper.selectById(rid);
                if (a != null && a.getStatus() != null && a.getStatus() == 1 && seen.add(rid)) {
                    result.add(toListItem(a));
                }
            }
        }
        int remain = Math.max(0, 5 - result.size());
        if (remain > 0) {
            List<Article> auto = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                    .eq(Article::getCategoryId, categoryId)
                    .eq(Article::getStatus, 1)
                    .ne(Article::getId, id)
                    .notIn(Article::getId, seen.isEmpty() ? List.of(-1L) : seen)
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT " + remain));
            for (Article a : auto) {
                if (seen.add(a.getId())) {
                    result.add(toListItem(a));
                }
            }
        }
        return result;
    }

    private VOs.ArticleBriefVO neighbor(Long id, Long categoryId, boolean next) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getCategoryId, categoryId).eq(Article::getStatus, 1);
        if (next) {
            qw.gt(Article::getId, id).orderByAsc(Article::getId);
        } else {
            qw.lt(Article::getId, id).orderByDesc(Article::getId);
        }
        qw.last("LIMIT 1");
        Article one = articleMapper.selectOne(qw);
        if (one == null) {
            return null;
        }
        VOs.ArticleBriefVO vo = new VOs.ArticleBriefVO();
        vo.setId(one.getId());
        vo.setSlug(one.getSlug());
        vo.setTitle(one.getTitle());
        return vo;
    }

    private VOs.ArticleListItemVO toListItem(Article a) {
        return VOs.ArticleListItemVO.builder()
                .id(a.getId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .categoryId(a.getCategoryId())
                .difficulty(a.getDifficulty())
                .tags(tagService.namesByArticleId(a.getId()))
                .viewCount(a.getViewCount())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private VOs.ArticleDetailVO toDetail(Article a, Category category) {
        return VOs.ArticleDetailVO.builder()
                .id(a.getId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .categoryId(a.getCategoryId())
                .categoryName(category.getName())
                .categorySlug(category.getSlug())
                .difficulty(a.getDifficulty())
                .tags(tagService.namesByArticleId(a.getId()))
                .contentHtml(a.getContentHtml())
                .toc(markdownService.extractToc(a.getContentHtml()))
                .viewCount(a.getViewCount())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    @Transactional
    public Article create(Requests.ArticleSaveDTO dto) {
        Category category = categoryService.getById(dto.getCategoryId());
        Article article = new Article();
        String slug = StringUtils.hasText(dto.getSlug()) ? dto.getSlug().trim() : category.getSlug() + "-" + System.currentTimeMillis();
        if (articleMapper.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getSlug, slug)) > 0) {
            throw new BizException(ErrorCode.CONFLICT, "slug 已存在");
        }
        article.setSlug(slug);
        applySave(article, dto);
        articleMapper.insert(article);
        tagService.replaceArticleTags(article.getId(), dto.getTags());
        replaceRelated(article.getId(), dto.getRelatedIds());
        categoryService.clearCache();
        return article;
    }

    @Transactional
    public Article update(Long id, Requests.ArticleSaveDTO dto) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        if (StringUtils.hasText(dto.getSlug())) {
            String slug = dto.getSlug().trim();
            if (!slug.equals(article.getSlug())
                    && articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                    .eq(Article::getSlug, slug).ne(Article::getId, id)) > 0) {
                throw new BizException(ErrorCode.CONFLICT, "slug 已存在");
            }
            article.setSlug(slug);
        }
        applySave(article, dto);
        articleMapper.updateById(article);
        tagService.replaceArticleTags(article.getId(), dto.getTags());
        replaceRelated(article.getId(), dto.getRelatedIds());
        categoryService.clearCache();
        return article;
    }

    private void replaceRelated(Long articleId, List<Long> relatedIds) {
        articleRelatedMapper.deleteByArticleId(articleId);
        if (relatedIds == null) {
            return;
        }
        for (Long rid : relatedIds) {
            if (rid == null || rid.equals(articleId)) {
                continue;
            }
            articleRelatedMapper.insertIgnore(articleId, rid);
            articleRelatedMapper.insertIgnore(rid, articleId);
        }
    }

    private void applySave(Article article, Requests.ArticleSaveDTO dto) {
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary() == null ? "" : dto.getSummary());
        article.setCategoryId(dto.getCategoryId());
        article.setDifficulty(normalizeDifficulty(dto.getDifficulty()));
        article.setStatus(1);
        article.setContentMd(dto.getContentMd() == null ? "" : dto.getContentMd());
        article.setContentHtml(markdownService.render(dto.getContentMd()));
    }

    private String normalizeDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return "MEDIUM";
        }
        return switch (difficulty.trim().toUpperCase()) {
            case "EASY", "简单" -> "EASY";
            case "HARD", "困难" -> "HARD";
            case "MEDIUM", "中等" -> "MEDIUM";
            default -> "MEDIUM";
        };
    }

    @Transactional
    public void delete(Long id) {
        articleRelatedMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);
        tagService.replaceArticleTags(id, List.of());
    }
}
