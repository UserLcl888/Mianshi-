package com.interview.controller;

import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.VOs;
import com.interview.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public Result<PageResult<VOs.ArticleListItemVO>> list(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(articleService.list(categoryId, difficulty, page, size));
    }

    @GetMapping("/{slug}")
    public Result<VOs.DetailRespVO> detail(@PathVariable("slug") String slug) {
        return Result.ok(articleService.detail(slug));
    }

    @PostMapping("/{id}/view")
    public Result<Long> recordView(@PathVariable("id") Long id) {
        return Result.ok(articleService.recordView(id));
    }
}
