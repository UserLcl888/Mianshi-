package com.interview.controller;

import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.VOs;
import com.interview.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习专题（公开）：分类板块聚合 + 板块内学习文章列表。
 */
@RestController
@RequestMapping("/api/learn")
@RequiredArgsConstructor
public class LearnController {

    private final ArticleService articleService;

    @GetMapping("/categories")
    public Result<List<VOs.LearnCategoryVO>> categories() {
        return Result.ok(articleService.learnCategories());
    }

    @GetMapping("/articles")
    public Result<PageResult<VOs.ArticleListItemVO>> articles(
            @RequestParam("categorySlug") String categorySlug,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "100") long size) {
        return Result.ok(articleService.learnList(categorySlug, page, size));
    }
}
