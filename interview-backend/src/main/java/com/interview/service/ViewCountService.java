package com.interview.service;

import com.interview.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览量聚合：每次浏览先 INCR Redis Hash（view:counter 的 articleId 字段），
 * 定时任务批量累加落库后清零，避免每个请求直接 UPDATE view_count 造成并发写放大。
 * 展示时取 DB 已落库值 + Redis 未落库增量，保证并发下计数准确。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {

    private static final String COUNTER_KEY = "view:counter";

    /** 取字段并删除的 Lua 脚本：GET + HDEL 原子执行；之后的 INCR 会从 1 重新累计，不会丢失。 */
    private static final String TAKE_SCRIPT =
            "local v = redis.call('HGET', KEYS[1], ARGV[1]) " +
            "if v then redis.call('HDEL', KEYS[1], ARGV[1]) return tonumber(v) end " +
            "return 0";

    private final StringRedisTemplate redis;
    private final ArticleMapper articleMapper;

    /**
     * 浏览 +1，返回该题目 Redis 中未落库的增量。
     */
    public long increment(Long articleId) {
        Long value = redis.opsForHash().increment(COUNTER_KEY, String.valueOf(articleId), 1);
        return value == null ? 0 : value;
    }

    /**
     * 批量取若干题目的未落库增量（pipeline，一次往返）。
     */
    public Map<Long, Long> countersOf(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object> fields = articleIds.stream().map(id -> (Object) String.valueOf(id)).toList();
        List<Object> values = redis.opsForHash().multiGet(COUNTER_KEY, fields);
        Map<Long, Long> result = new HashMap<>();
        int i = 0;
        for (Long id : articleIds) {
            Object raw = i < values.size() ? values.get(i) : null;
            if (raw != null) {
                try {
                    result.put(id, Long.parseLong(raw.toString()));
                } catch (NumberFormatException ignored) {
                }
            }
            i++;
        }
        return result;
    }

    /**
     * 单题目未落库增量。
     */
    public long counterOf(Long articleId) {
        Object value = redis.opsForHash().get(COUNTER_KEY, String.valueOf(articleId));
        if (value == null || !StringUtils.hasText(value.toString())) {
            return 0;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 全部未落库增量总和（首页总浏览量叠加用）。
     */
    public long totalUnflushed() {
        Map<Object, Object> entries = redis.opsForHash().entries(COUNTER_KEY);
        long total = 0;
        for (Object value : entries.values()) {
            if (value == null) {
                continue;
            }
            try {
                total += Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return total;
    }

    /**
     * 定时聚合落库：读取快照 -> 对每个字段原子 GET+DEL -> 累加进 DB。
     * 先取后写：取走后再发生的浏览从 1 重新累计，不会丢；DB 写失败仅记录日志（计数丢失概率极小）。
     */
    @Scheduled(fixedDelayString = "${app.view.flush-interval-ms:60000}")
    public void flushToDb() {
        Map<Object, Object> entries = redis.opsForHash().entries(COUNTER_KEY);
        if (entries.isEmpty()) {
            return;
        }
        List<Long> ids = new ArrayList<>();
        long total = 0;
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            try {
                Long id = Long.parseLong(e.getKey().toString());
                ids.add(id);
            } catch (NumberFormatException ignored) {
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            Long delta = takeCounter(id);
            if (delta == null || delta <= 0) {
                continue;
            }
            try {
                articleMapper.incrementViewCount(id, delta);
                total += delta;
            } catch (Exception ex) {
                log.warn("浏览量落库失败 articleId={} delta={}", id, delta, ex);
            }
        }
        log.info("浏览量聚合落库完成：{} 篇题目，共 {} 次", ids.size(), total);
    }

    /**
     * 原子取走字段值并删除。
     */
    private Long takeCounter(Long articleId) {
        try {
            byte[] key = keySerializer().serialize(COUNTER_KEY);
            byte[] field = keySerializer().serialize(String.valueOf(articleId));
            Object result = redis.execute((RedisCallback<Object>) connection ->
                    connection.eval(TAKE_SCRIPT.getBytes(), ReturnType.INTEGER, 1, key, field));
            return result == null ? null : Long.valueOf(result.toString());
        } catch (Exception ex) {
            log.warn("清理 Redis 浏览量字段失败 articleId={}", articleId, ex);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private RedisSerializer<String> keySerializer() {
        return (RedisSerializer<String>) redis.getKeySerializer();
    }
}
