package com.interview.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.PageResult;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.entity.Category;
import com.interview.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);

    private final ArticleMapper articleMapper;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final MarkdownService markdownService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ContentCacheService contentCacheService;
    private final ViewCountService viewCountService;
    private final AdminLogService adminLogService;

    public PageResult<VOs.ArticleListItemVO> list(Long categoryId, String difficulty, long page, long size) {
        String difficultyNorm = StringUtils.hasText(difficulty) ? normalizeDifficulty(difficulty) : null;
        String cacheKey = listCacheKey(categoryId, difficultyNorm, page, size);
        PageResult<VOs.ArticleListItemVO> cached = readListCache(cacheKey);
        if (cached != null) {
            refreshViewCounts(cached.getList());
            return cached;
        }

        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getStatus, 1);
        if (StringUtils.hasText(difficultyNorm)) {
            qw.eq(Article::getDifficulty, difficultyNorm);
        }
        if (categoryId != null) {
            qw.in(Article::getCategoryId, categoryService.collectIds(categoryId));
        }
        trimLongTextColumns(qw);
        qw.orderByAsc(Article::getId);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), qw);
        PageResult<VOs.ArticleListItemVO> pageResult = toPageResult(result, page, size);
        writeListCache(cacheKey, pageResult);
        refreshViewCounts(pageResult.getList());
        return pageResult;
    }

    /**
     * 管理端题目列表：不限制发布状态，按 id 倒序。
     */
    public PageResult<VOs.ArticleListItemVO> adminList(Long categoryId, String difficulty, long page, long size) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(difficulty)) {
            qw.eq(Article::getDifficulty, normalizeDifficulty(difficulty));
        }
        if (categoryId != null) {
            qw.in(Article::getCategoryId, categoryService.collectIds(categoryId));
        }
        trimLongTextColumns(qw);
        qw.orderByDesc(Article::getId);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), qw);
        PageResult<VOs.ArticleListItemVO> pageResult = toPageResult(result, page, size);
        refreshViewCounts(pageResult.getList());
        return pageResult;
    }

    public VOs.DetailRespVO detail(String slug) {
        String cacheKey = detailCacheKey(slug);
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                VOs.DetailRespVO resp = objectMapper.readValue(cached, new TypeReference<VOs.DetailRespVO>() {
                });
                if (resp.getArticle() != null) {
                    resp.getArticle().setViewCount(displayViewCount(resp.getArticle().getId()));
                }
                return resp;
            } catch (JsonProcessingException ignored) {
            }
        }

        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, slug).eq(Article::getStatus, 1));
        if (article == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或已下架");
        }
        Category category = categoryService.getById(article.getCategoryId());
        VOs.ArticleDetailVO detailVO = toDetail(article, category);

        VOs.DetailRespVO resp = new VOs.DetailRespVO();
        resp.setArticle(detailVO);
        resp.setPrev(neighbor(article.getId(), article.getCategoryId(), false));
        resp.setNext(neighbor(article.getId(), article.getCategoryId(), true));
        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(resp), DETAIL_CACHE_TTL);
        } catch (JsonProcessingException ignored) {
        }
        detailVO.setViewCount(displayViewCount(article.getId()));
        return resp;
    }

    /**
     * 浏览上报：同一用户 24 小时内对同一题目只计一次浏览。
     * 计数先进 Redis 聚合，由定时任务批量落库；返回值为 DB + Redis 的实时总量，
     * 并发下不再依赖 +1 的近似值。
     */
    public Long recordView(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或已下架");
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String dedupKey = "view:user:" + userId + ":article:" + articleId;
        Boolean first = redis.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofHours(24));
        if (Boolean.TRUE.equals(first)) {
            viewCountService.increment(articleId);
        }
        return displayViewCount(articleId);
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

    private PageResult<VOs.ArticleListItemVO> toPageResult(Page<Article> result, long page, long size) {
        List<Long> ids = result.getRecords().stream().map(Article::getId).toList();
        Map<Long, List<String>> tagsByArticle = tagService.namesByArticleIds(ids);
        List<VOs.ArticleListItemVO> list = result.getRecords().stream()
                .map(a -> toListItem(a, tagsByArticle.getOrDefault(a.getId(), List.of())))
                .toList();
        return PageResult.of(page, size, result.getTotal(), list);
    }

    private VOs.ArticleListItemVO toListItem(Article a, List<String> tags) {
        return VOs.ArticleListItemVO.builder()
                .id(a.getId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .categoryId(a.getCategoryId())
                .difficulty(a.getDifficulty())
                .tags(tags)
                .viewCount(a.getViewCount())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private VOs.ArticleDetailVO toDetail(Article a, Category category) {
        Map<Long, List<String>> tagsByArticle = tagService.namesByArticleIds(List.of(a.getId()));
        return VOs.ArticleDetailVO.builder()
                .id(a.getId())
                .slug(a.getSlug())
                .title(a.getTitle())
                .summary(a.getSummary())
                .docUrl(a.getDocUrl())
                .categoryId(a.getCategoryId())
                .categoryName(category.getName())
                .categorySlug(category.getSlug())
                .difficulty(a.getDifficulty())
                .tags(tagsByArticle.getOrDefault(a.getId(), List.of()))
                .contentMd(a.getContentMd())
                .contentHtml(a.getContentHtml())
                .toc(markdownService.extractToc(a.getContentHtml()))
                .viewCount(a.getViewCount())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    /**
     * 批量刷新列表项的实时浏览量：DB 已落库值 + Redis 未落库增量。
     */
    private void refreshViewCounts(List<VOs.ArticleListItemVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> ids = items.stream().map(VOs.ArticleListItemVO::getId).toList();
        Map<Long, Long> dbCounts = loadDbViewCounts(ids);
        Map<Long, Long> increments = viewCountService.countersOf(ids);
        for (VOs.ArticleListItemVO item : items) {
            long base = dbCounts.getOrDefault(item.getId(), item.getViewCount() == null ? 0L : item.getViewCount());
            item.setViewCount(base + increments.getOrDefault(item.getId(), 0L));
        }
    }

    private long displayViewCount(Long articleId) {
        Map<Long, Long> dbCounts = loadDbViewCounts(List.of(articleId));
        long base = dbCounts.getOrDefault(articleId, 0L);
        return base + viewCountService.counterOf(articleId);
    }

    private Map<Long, Long> loadDbViewCounts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return articleMapper.selectViewCounts(ids).stream().collect(Collectors.toMap(
                row -> ((Number) row.get("id")).longValue(),
                row -> ((Number) row.get("viewCount")).longValue()));
    }

    @Transactional
    public VOs.ArticleVO create(Requests.ArticleSaveDTO dto) {
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
        categoryService.clearCache();
        contentCacheService.bump();
        Article saved = articleMapper.selectById(article.getId());
        adminLogService.write("ARTICLE_CREATE", "ARTICLE", saved.getId(), "新增题目：" + saved.getTitle());
        return toVO(saved);
    }

    @Transactional
    public VOs.ArticleVO update(Long id, Requests.ArticleSaveDTO dto) {
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
        article.setUpdatedAt(LocalDateTime.now());
        articleMapper.updateById(article);
        tagService.replaceArticleTags(article.getId(), dto.getTags());
        categoryService.clearCache();
        contentCacheService.bump();
        adminLogService.write("ARTICLE_UPDATE", "ARTICLE", article.getId(), "编辑题目：" + article.getTitle());
        return toVO(article);
    }

    /**
     * 列表查询只取展示列，排除 content_md/content_html 两个 LONGTEXT，减少分页传输量。
     */
    private void trimLongTextColumns(LambdaQueryWrapper<Article> qw) {
        qw.select(Article::getId, Article::getSlug, Article::getTitle, Article::getSummary,
                Article::getDocUrl, Article::getCategoryId, Article::getDifficulty, Article::getStatus,
                Article::getViewCount, Article::getCreatedBy, Article::getCreatedAt, Article::getUpdatedAt);
    }

    private VOs.ArticleVO toVO(Article article) {
        return VOs.ArticleVO.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(article.getTitle())
                .summary(article.getSummary())
                .docUrl(article.getDocUrl())
                .categoryId(article.getCategoryId())
                .difficulty(article.getDifficulty())
                .status(article.getStatus())
                .viewCount(article.getViewCount())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private void applySave(Article article, Requests.ArticleSaveDTO dto) {
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary() == null ? "" : dto.getSummary());
        article.setDocUrl(dto.getDocUrl() == null ? "" : dto.getDocUrl().trim());
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
        articleMapper.deleteById(id);
        tagService.replaceArticleTags(id, List.of());
        contentCacheService.bump();
        adminLogService.write("ARTICLE_DELETE", "ARTICLE", id, "删除题目");
    }

    private String listCacheKey(Long categoryId, String difficulty, long page, long size) {
        return "cache:article:list:v" + contentCacheService.version()
                + ":" + categoryId + ":" + (difficulty == null ? "" : difficulty)
                + ":" + page + ":" + size;
    }

    private String detailCacheKey(String slug) {
        return "cache:article:detail:v" + contentCacheService.version() + ":" + slug;
    }

    private PageResult<VOs.ArticleListItemVO> readListCache(String cacheKey) {
        String cached = redis.opsForValue().get(cacheKey);
        if (cached == null) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, new TypeReference<PageResult<VOs.ArticleListItemVO>>() {
            });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void writeListCache(String cacheKey, PageResult<VOs.ArticleListItemVO> result) {
        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), LIST_CACHE_TTL);
        } catch (JsonProcessingException ignored) {
        }
    }
}
