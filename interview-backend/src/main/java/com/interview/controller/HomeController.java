package com.interview.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.common.Result;
import com.interview.entity.Article;
import com.interview.entity.ArticleTag;
import com.interview.entity.Category;
import com.interview.entity.DailyQuote;
import com.interview.entity.Tag;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.ArticleTagMapper;
import com.interview.mapper.CategoryMapper;
import com.interview.mapper.DailyQuoteMapper;
import com.interview.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagService tagService;
    private final ArticleTagMapper articleTagMapper;
    private final DailyQuoteMapper dailyQuoteMapper;

    /**
     * 每日一句：按日期轮换（当天固定，次日切换）。
     */
    @GetMapping("/quote")
    public Result<Map<String, String>> quote() {
        List<DailyQuote> list = dailyQuoteMapper.selectList(
                new LambdaQueryWrapper<DailyQuote>().orderByAsc(DailyQuote::getId));
        if (list.isEmpty()) {
            return Result.ok(Map.of());
        }
        long day = LocalDate.now().toEpochDay();
        DailyQuote q = list.get((int) Math.floorMod(day, list.size()));
        return Result.ok(Map.of(
                "content", q.getContent() == null ? "" : q.getContent(),
                "author", q.getAuthor() == null ? "" : q.getAuthor()));
    }

    /**
     * 首页公开数据：站点统计 + 热门文档 + 热门标签。
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        List<Article> published = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));
        long articleCount = published.size();
        long viewCount = published.stream()
                .mapToLong(a -> a.getViewCount() == null ? 0L : a.getViewCount())
                .sum();

        List<Map<String, Object>> hotArticles = new ArrayList<>();
        List<Article> top = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT 8"));
        for (Article a : top) {
            Category c = categoryMapper.selectById(a.getCategoryId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("slug", a.getSlug());
            m.put("title", a.getTitle());
            m.put("categoryName", c == null ? "" : c.getName());
            m.put("viewCount", a.getViewCount());
            hotArticles.add(m);
        }

        List<Map<String, Object>> hotTags = new ArrayList<>();
        tagService.listAll().stream()
                .map(t -> Map.entry(t, articleTagMapper.selectCount(
                        new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, t.getId()))))
                .sorted((x, y) -> Long.compare(y.getValue(), x.getValue()))
                .limit(15)
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey().getName());
                    m.put("count", e.getValue());
                    hotTags.add(m);
                });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("articleCount", articleCount);
        data.put("viewCount", viewCount);
        data.put("hotArticles", hotArticles);
        data.put("hotTags", hotTags);
        return Result.ok(data);
    }
}
