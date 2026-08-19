package com.interview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 内容缓存版本号：题目/标签/分类变更时 +1，列表、详情、首页统计缓存 key 携带版本号，
 * 避免手工按模式删除 Redis key，同时保证缓存即时失效。
 */
@Service
@RequiredArgsConstructor
public class ContentCacheService {

    private static final String VERSION_KEY = "cache:content:version";

    private final StringRedisTemplate redis;

    public long version() {
        String value = redis.opsForValue().get(VERSION_KEY);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void bump() {
        redis.opsForValue().increment(VERSION_KEY);
    }
}
