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
}
