package com.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.entity.Category;
import com.interview.mapper.ArticleMapper;
import com.interview.mapper.CategoryMapper;
import com.interview.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", userMapper.selectCount(null));
        data.put("articleCount", articleMapper.selectCount(null));
        data.put("categoryCount", categoryMapper.selectCount(null));
        data.put("publishedCount", articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1)));
        return data;
    }

    public List<VOs.TopArticleVO> topArticles(int limit) {
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 50)));
        return articles.stream().map(a -> {
            VOs.TopArticleVO vo = new VOs.TopArticleVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setViewCount(a.getViewCount());
            Category category = categoryMapper.selectById(a.getCategoryId());
            vo.setCategoryName(category == null ? "" : category.getName());
            return vo;
        }).toList();
    }

    /**
     * 各顶级分类的统计：聚合其全部子分类的题目数与浏览量。
     */
    public List<VOs.CategoryStatsVO> categoryStats() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
        List<Article> articles = articleMapper.selectList(null);

        Map<Long, Integer> countByCat = new HashMap<>();
        Map<Long, Long> viewsByCat = new HashMap<>();
        for (Article a : articles) {
            countByCat.merge(a.getCategoryId(), 1, Integer::sum);
            viewsByCat.merge(a.getCategoryId(), a.getViewCount() == null ? 0L : a.getViewCount(), Long::sum);
        }

        Map<Long, List<Long>> children = new HashMap<>();
        for (Category c : all) {
            children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
        }

        List<VOs.CategoryStatsVO> result = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() != null && c.getParentId() != 0L) {
                continue;
            }
            List<Long> ids = new ArrayList<>();
            Deque<Long> stack = new ArrayDeque<>();
            stack.push(c.getId());
            while (!stack.isEmpty()) {
                Long id = stack.pop();
                ids.add(id);
                List<Long> subs = children.get(id);
                if (subs != null) {
                    subs.forEach(stack::push);
                }
            }
            int count = 0;
            long views = 0;
            for (Long id : ids) {
                count += countByCat.getOrDefault(id, 0);
                views += viewsByCat.getOrDefault(id, 0L);
            }
            result.add(VOs.CategoryStatsVO.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .viewCount(views)
                    .articleCount(count)
                    .build());
        }
        result.sort(Comparator.comparingLong(VOs.CategoryStatsVO::getViewCount).reversed());
        return result;
    }
}
