package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.RedisKeys;
import com.interview.dto.Rows;
import com.interview.entity.DailyQuote;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.ArticleTagMapper;
import com.interview.mapper.DailyQuoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页公开数据：每日一句 + 站点统计/热门文档/热门标签。
 * 聚合全部收敛为少量 SQL（统计 1 条、热门文档 1 条 JOIN、热门标签 1 条 JOIN），
 * 结果整体缓存 Redis，读取时叠加 Redis 未落库浏览量，保证实时性。
 */
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final DailyQuoteMapper dailyQuoteMapper;
    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ViewCountService viewCountService;
    private final ContentCacheService contentCacheService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    /**
     * 每日一句：按日期轮换（当天固定，次日切换）。
     */
    public Map<String, String> quote() {
        List<DailyQuote> list = dailyQuoteMapper.selectList(
                new LambdaQueryWrapper<DailyQuote>().orderByAsc(DailyQuote::getId));
        if (list.isEmpty()) {
            return Map.of();
        }
        long day = LocalDate.now().toEpochDay();
        DailyQuote q = list.get((int) Math.floorMod(day, list.size()));
        return Map.of(
                "content", q.getContent() == null ? "" : q.getContent(),
                "author", q.getAuthor() == null ? "" : q.getAuthor());
    }

    /**
     * 首页公开数据：站点统计 + 热门文档 + 热门标签。
     */
    public Map<String, Object> overview() {
        String cacheKey = RedisKeys.homeOverviewKey(contentCacheService.version());
        String cached = redis.opsForValue().get(cacheKey);
        Map<String, Object> data = null;
        if (cached != null) {
            try {
                data = objectMapper.readValue(cached, new TypeReference<Map<String, Object>>() {
                });
            } catch (JsonProcessingException ignored) {
            }
        }
        if (data == null) {
            data = buildOverview();
            try {
                redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(data), CACHE_TTL);
            } catch (JsonProcessingException ignored) {
            }
        }
        enrichViewCounts(data);
        return data;
    }

    /**
     * 全量聚合（缓存 miss 时执行）：统计 1 条 SQL、热门文档 1 条 JOIN、热门标签 1 条 JOIN。
     */
    private Map<String, Object> buildOverview() {
        Rows.PublishedStatsRow stats = articleMapper.selectPublishedStats();
        long articleCount = stats.getArticleCount() == null ? 0L : stats.getArticleCount();
        long viewCount = stats.getViewCount() == null ? 0L : stats.getViewCount();

        List<Map<String, Object>> hotArticles = articleMapper.selectTopPublished(10).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("slug", r.getSlug());
            m.put("title", r.getTitle());
            m.put("categoryName", r.getCategoryName());
            m.put("viewCount", r.getViewCount() == null ? 0L : r.getViewCount());
            return m;
        }).toList();
        List<Map<String, Object>> hotTags = articleTagMapper.selectHotTags(15).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", r.getName());
            m.put("count", r.getCount() == null ? 0L : r.getCount());
            return m;
        }).toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleCount", articleCount);
        data.put("viewCount", viewCount);
        data.put("hotArticles", hotArticles);
        data.put("hotTags", hotTags);
        return data;
    }

    /**
     * 读取时叠加 Redis 未落库浏览量：总浏览量 = DB 总和 + 全部未落库增量；
     * 热门文档每篇 = DB 实时值 + 该篇未落库增量。
     */
    @SuppressWarnings("unchecked")
    private void enrichViewCounts(Map<String, Object> data) {
        Rows.PublishedStatsRow stats = articleMapper.selectPublishedStats();
        long baseTotal = stats.getViewCount() == null ? 0L : stats.getViewCount();
        data.put("viewCount", baseTotal + viewCountService.totalUnflushed());

        List<Map<String, Object>> hotArticles = (List<Map<String, Object>>) data.get("hotArticles");
        if (hotArticles == null || hotArticles.isEmpty()) {
            return;
        }
        List<Long> ids = hotArticles.stream()
                .map(m -> ((Number) m.get("id")).longValue())
                .toList();
        Map<Long, Long> dbCounts = articleMapper.selectViewCounts(ids).stream().collect(Collectors.toMap(
                Rows.ViewCountRow::getId,
                row -> row.getViewCount() == null ? 0L : row.getViewCount()));
        Map<Long, Long> increments = viewCountService.countersOf(ids);
        for (Map<String, Object> item : hotArticles) {
            Long id = ((Number) item.get("id")).longValue();
            long base = dbCounts.getOrDefault(id, 0L);
            item.put("viewCount", base + increments.getOrDefault(id, 0L));
        }
    }
}
