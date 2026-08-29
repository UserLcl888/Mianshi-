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
import com.interview.common.RedisKeys;
import com.interview.dto.Requests;
import com.interview.dto.Rows;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.entity.Category;
import com.interview.entity.LearnCategory;
import com.interview.entity.User;
import com.interview.enums.AdminLogAction;
import com.interview.enums.Difficulty;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.LearnCategoryMapper;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);

    private final ArticleMapper articleMapper;
    private final LearnCategoryMapper learnCategoryMapper;
    private final CategoryService categoryService;
    private final UserMapper userMapper;
    private final AccessService accessService;
    private final TagService tagService;
    private final MarkdownService markdownService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ContentCacheService contentCacheService;
    private final ViewCountService viewCountService;
    private final AdminLogService adminLogService;

    public PageResult<VOs.ArticleListItemVO> list(Long categoryId, String difficulty, long page, long size) {
        // 受限分类：列表查询同样走权限校验，未授权（含游客/普通用户）不允许拉取任何题目
        if (categoryId != null) {
            Category category = categoryService.getById(categoryId);
            User viewer = StpUtil.isLogin() ? userMapper.selectById(StpUtil.getLoginIdAsLong()) : null;
            accessService.checkArticleAccess(viewer, null, category);
        }
        String difficultyNorm = StringUtils.hasText(difficulty) ? Difficulty.normalize(difficulty).getCode() : null;
        String cacheKey = listCacheKey(categoryId, difficultyNorm, page, size);
        PageResult<VOs.ArticleListItemVO> cached = readListCache(cacheKey);
        if (cached != null) {
            refreshViewCounts(cached.getList());
            return cached;
        }

        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getStatus, 1);
        // 首页/分类列表只展示技术问题专栏，专题分享文章不进普通列表
        qw.eq(Article::getColumnType, "tech");
        if (StringUtils.hasText(difficultyNorm)) {
            qw.eq(Article::getDifficulty, difficultyNorm);
        }
        if (categoryId != null) {
            qw.in(Article::getCategoryId, categoryService.collectIds(categoryId));
        }
        trimLongTextColumns(qw);
        qw.orderByAsc(Article::getSortOrder).orderByAsc(Article::getId);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), qw);
        PageResult<VOs.ArticleListItemVO> pageResult = toPageResult(result, page, size);
        writeListCache(cacheKey, pageResult);
        refreshViewCounts(pageResult.getList());
        return pageResult;
    }

    /**
     * 管理端题目列表：不限制发布状态，按 id 倒序。
     */
    public PageResult<VOs.ArticleListItemVO> adminList(String columnType, Long categoryId, String difficulty,
                                                       long page, long size) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(columnType)) {
            qw.eq(Article::getColumnType, normalizeColumnType(columnType));
        }
        if (StringUtils.hasText(difficulty)) {
            qw.eq(Article::getDifficulty, Difficulty.normalize(difficulty).getCode());
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

    /**
     * 专题分享专栏列表（公开）：置顶优先，其次手动排序，最后按 id 倒序。
     */
    public PageResult<VOs.ArticleListItemVO> topicList(String keyword, long page, long size) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getColumnType, "topic").eq(Article::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            qw.like(Article::getTitle, keyword.trim());
        }
        trimLongTextColumns(qw);
        qw.orderByDesc(Article::getIsPinned)
                .orderByAsc(Article::getSortOrder)
                .orderByDesc(Article::getId);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), qw);
        PageResult<VOs.ArticleListItemVO> pageResult = toPageResult(result, page, size);
        refreshViewCounts(pageResult.getList());
        return pageResult;
    }

    /**
     * 学习专题：按独立学习分类聚合的学习板块（AI / Java / MySQL …）。
     */
    public List<VOs.LearnCategoryVO> learnCategories() {
        List<LearnCategory> cats = learnCategoryMapper.selectList(new LambdaQueryWrapper<LearnCategory>()
                .orderByAsc(LearnCategory::getSortOrder)
                .orderByAsc(LearnCategory::getId));
        if (cats.isEmpty()) {
            return List.of();
        }
        List<Long> ids = cats.stream().map(LearnCategory::getId).toList();
        List<Article> learnArticles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getLearnCategoryId, Article::getUpdatedAt)
                .eq(Article::getColumnType, "learn")
                .eq(Article::getStatus, 1)
                .in(Article::getLearnCategoryId, ids));
        Map<Long, List<Article>> byCat = learnArticles.stream()
                .collect(Collectors.groupingBy(a -> a.getLearnCategoryId() == null ? -1L : a.getLearnCategoryId()));
        return cats.stream()
                .map(c -> {
                    List<Article> list = byCat.getOrDefault(c.getId(), List.of());
                    LocalDateTime max = list.stream()
                            .map(Article::getUpdatedAt)
                            .filter(Objects::nonNull)
                            .max(LocalDateTime::compareTo)
                            .orElse(null);
                    return VOs.LearnCategoryVO.builder()
                            .id(c.getId())
                            .slug(c.getSlug())
                            .name(c.getName())
                            .articleCount((long) list.size())
                            .updatedAt(max == null ? null : max.toString())
                            .build();
                })
                .toList();
    }

    /**
     * 学习专题：某分类板块下的学习文章列表。
     */
    public PageResult<VOs.ArticleListItemVO> learnList(String categorySlug, long page, long size) {
        LearnCategory learnCategory = learnCategoryMapper.selectOne(
                new LambdaQueryWrapper<LearnCategory>().eq(LearnCategory::getSlug, categorySlug));
        if (learnCategory == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "学习分类不存在");
        }
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getColumnType, "learn").eq(Article::getStatus, 1)
                .eq(Article::getLearnCategoryId, learnCategory.getId());
        trimLongTextColumns(qw);
        qw.orderByAsc(Article::getSortOrder).orderByAsc(Article::getId);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, size), qw);
        PageResult<VOs.ArticleListItemVO> pageResult = toPageResult(result, page, size);
        refreshViewCounts(pageResult.getList());
        return pageResult;
    }

    public VOs.DetailRespVO detail(String slug) {
        // 先查文章并做访问权限校验：受限内容不能命中缓存跳过校验（审批撤销后立即失效）
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, slug).eq(Article::getStatus, 1));
        if (article == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或已下架");
        }
        boolean unrestricted = "topic".equals(article.getColumnType()) || "learn".equals(article.getColumnType());
        if (!unrestricted) {
            // 技术问题专栏：沿用分类访问权限；文章/学习专栏公开可看，不做权限校验
            Category category = categoryService.getById(article.getCategoryId());
            User viewer = StpUtil.isLogin() ? userMapper.selectById(StpUtil.getLoginIdAsLong()) : null;
            accessService.checkArticleAccess(viewer, article, category);
        }
        Category category = unrestricted ? null : categoryService.getById(article.getCategoryId());

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

        VOs.ArticleDetailVO detailVO = toDetail(article, category);

        VOs.DetailRespVO resp = new VOs.DetailRespVO();
        resp.setArticle(detailVO);
        resp.setPrev(neighbor(article.getId(), article.getCategoryId(), article.getColumnType(), false));
        resp.setNext(neighbor(article.getId(), article.getCategoryId(), article.getColumnType(), true));
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
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getStatus)
                .eq(Article::getId, articleId));
        if (article == null || article.getStatus() == null || article.getStatus() != 1) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在或已下架");
        }
        Long userId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        if (userId != null) {
            String dedupKey = RedisKeys.viewDedup(userId, articleId);
            Boolean first = redis.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofHours(24));
            if (Boolean.TRUE.equals(first)) {
                viewCountService.increment(articleId);
            }
        } else {
            // 游客浏览：直接计数（不做 24h 去重）
            viewCountService.increment(articleId);
        }
        return displayViewCount(articleId);
    }

    private VOs.ArticleBriefVO neighbor(Long id, Long categoryId, String columnType, boolean next) {
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<>();
        qw.eq(Article::getStatus, 1).eq(Article::getColumnType, columnType);
        if (!"topic".equals(columnType) && !"learn".equals(columnType)) {
            qw.eq(Article::getCategoryId, categoryId);
        }
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
                .columnType(a.getColumnType())
                .categoryId(a.getCategoryId())
                .difficulty(a.getDifficulty())
                .isPinned(a.getIsPinned())
                .coverUrl(a.getCoverUrl())
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
                .columnType(a.getColumnType())
                .learnCategoryId(a.getLearnCategoryId())
                .categoryId(a.getCategoryId())
                .categoryName(category == null
                        ? fallbackCategoryName(a.getColumnType())
                        : category.getName())
                .categorySlug(category == null ? "" : category.getSlug())
                .difficulty(a.getDifficulty())
                .isPinned(a.getIsPinned())
                .coverUrl(a.getCoverUrl())
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
                Rows.ViewCountRow::getId,
                Rows.ViewCountRow::getViewCount));
    }

    @Transactional
    public VOs.ArticleVO create(Requests.ArticleSaveDTO dto) {
        String columnType = normalizeColumnType(dto.getColumnType());
        dto.setColumnType(columnType);
        if ("learn".equals(columnType)) {
            if (dto.getLearnCategoryId() == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "请选择学习分类");
            }
            if (dto.getCategoryId() == null) {
                dto.setCategoryId(0L);
            }
        } else if ("topic".equals(columnType)) {
            if (dto.getCategoryId() == null) {
                dto.setCategoryId(0L);
            }
        } else if (dto.getCategoryId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择所属分类");
        }
        Category category = ("topic".equals(columnType) || "learn".equals(columnType))
                ? null
                : categoryService.getById(dto.getCategoryId());
        Article article = new Article();
        String slug;
        if (StringUtils.hasText(dto.getSlug())) {
            slug = dto.getSlug().trim();
        } else if ("topic".equals(columnType)) {
            slug = "topic-" + System.currentTimeMillis();
        } else if ("learn".equals(columnType)) {
            slug = "learn-" + System.currentTimeMillis();
        } else {
            slug = category.getSlug() + "-" + System.currentTimeMillis();
        }
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
        adminLogService.write(AdminLogAction.ARTICLE_CREATE, saved.getId(), "新增题目：" + saved.getTitle());
        return toVO(saved);
    }

    @Transactional
    public VOs.ArticleVO update(Long id, Requests.ArticleSaveDTO dto) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        String columnType = normalizeColumnType(dto.getColumnType());
        dto.setColumnType(columnType);
        if ("learn".equals(columnType)) {
            if (dto.getLearnCategoryId() == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "请选择学习分类");
            }
            if (dto.getCategoryId() == null) {
                dto.setCategoryId(0L);
            }
        } else if ("topic".equals(columnType)) {
            if (dto.getCategoryId() == null) {
                dto.setCategoryId(0L);
            }
        } else if (dto.getCategoryId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择所属分类");
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
        adminLogService.write(AdminLogAction.ARTICLE_UPDATE, article.getId(), "编辑题目：" + article.getTitle());
        return toVO(article);
    }

    /**
     * 列表查询只取展示列，排除 content_md/content_html 两个 LONGTEXT，减少分页传输量。
     */
    private void trimLongTextColumns(LambdaQueryWrapper<Article> qw) {
        qw.select(Article::getId, Article::getSlug, Article::getTitle, Article::getSummary,
                Article::getDocUrl, Article::getColumnType, Article::getCategoryId, Article::getDifficulty,
                Article::getStatus, Article::getIsPinned, Article::getCoverUrl,
                Article::getViewCount, Article::getCreatedBy, Article::getCreatedAt, Article::getUpdatedAt);
    }

    private VOs.ArticleVO toVO(Article article) {
        return VOs.ArticleVO.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(article.getTitle())
                .summary(article.getSummary())
                .docUrl(article.getDocUrl())
                .columnType(article.getColumnType())
                .categoryId(article.getCategoryId())
                .difficulty(article.getDifficulty())
                .status(article.getStatus())
                .isPinned(article.getIsPinned())
                .coverUrl(article.getCoverUrl())
                .viewCount(article.getViewCount())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private void applySave(Article article, Requests.ArticleSaveDTO dto) {
        article.setTitle(dto.getTitle());
        article.setSummary(dto.getSummary() == null ? "" : dto.getSummary());
        article.setDocUrl(dto.getDocUrl() == null ? "" : dto.getDocUrl().trim());
        article.setColumnType(normalizeColumnType(dto.getColumnType()));
        article.setLearnCategoryId("learn".equals(article.getColumnType()) ? dto.getLearnCategoryId() : null);
        article.setCategoryId(dto.getCategoryId() == null ? 0L : dto.getCategoryId());
        article.setDifficulty(Difficulty.normalize(dto.getDifficulty()).getCode());
        article.setStatus(1);
        article.setIsPinned(dto.getIsPinned() != null && dto.getIsPinned() == 1 ? 1 : 0);
        article.setCoverUrl(dto.getCoverUrl() == null ? "" : dto.getCoverUrl().trim());
        article.setContentMd(dto.getContentMd() == null ? "" : dto.getContentMd());
        article.setContentHtml(markdownService.render(dto.getContentMd()));
    }

    private String normalizeColumnType(String columnType) {
        if ("topic".equals(columnType)) {
            return "topic";
        }
        if ("learn".equals(columnType)) {
            return "learn";
        }
        return "tech";
    }

    private String fallbackCategoryName(String columnType) {
        if ("topic".equals(columnType)) {
            return "文章分享";
        }
        if ("learn".equals(columnType)) {
            return "学习专题";
        }
        return "";
    }

    @Transactional
    public void delete(Long id) {
        articleMapper.deleteById(id);
        tagService.replaceArticleTags(id, List.of());
        contentCacheService.bump();
        adminLogService.write(AdminLogAction.ARTICLE_DELETE, id, "删除题目");
    }

    /**
     * 批量调整题目顺序（同一分类下生效，列表按 sort_order 升序展示）。
     */
    @Transactional
    public void reorder(List<Requests.ArticleReorderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (Requests.ArticleReorderItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            Article article = articleMapper.selectById(item.getId());
            if (article == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "题目不存在：" + item.getId());
            }
            article.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            articleMapper.updateById(article);
        }
        contentCacheService.bump();
    }

    /**
     * 专题分享专栏排序 + 置顶：仅允许操作 column_type=topic 的文章。
     */
    @Transactional
    public void reorderTopics(List<Requests.TopicReorderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (Requests.TopicReorderItem item : items) {
            if (item.getId() == null) {
                continue;
            }
            Article article = articleMapper.selectById(item.getId());
            if (article == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "题目不存在：" + item.getId());
            }
            if (!"topic".equals(article.getColumnType())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "仅支持对专题分享文章排序");
            }
            article.setSortOrder(item.getSortOrder() == null ? 0 : item.getSortOrder());
            article.setIsPinned(item.getIsPinned() != null && item.getIsPinned() == 1 ? 1 : 0);
            articleMapper.updateById(article);
        }
        contentCacheService.bump();
    }

    private String listCacheKey(Long categoryId, String difficulty, long page, long size) {
        return RedisKeys.articleListKey(contentCacheService.version(), categoryId, difficulty, page, size);
    }

    private String detailCacheKey(String slug) {
        return RedisKeys.articleDetailKey(contentCacheService.version(), slug);
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
