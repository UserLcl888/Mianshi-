package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/articles")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    @GetMapping
    public Result<PageResult<VOs.ArticleListItemVO>> list(
            @RequestParam(value = "columnType", required = false) String columnType,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "difficulty", required = false) String difficulty,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(articleService.adminList(columnType, categoryId, difficulty, page, size));
    }

    @PostMapping
    public Result<VOs.ArticleVO> create(@Valid @RequestBody Requests.ArticleSaveDTO dto) {
        return Result.ok(articleService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<VOs.ArticleVO> update(@PathVariable("id") Long id, @Valid @RequestBody Requests.ArticleSaveDTO dto) {
        return Result.ok(articleService.update(id, dto));
    }

    @PutMapping("/reorder")
    public Result<Void> reorder(@RequestBody List<Requests.ArticleReorderItem> items) {
        articleService.reorder(items);
        return Result.ok();
    }

    @PutMapping("/topics/reorder")
    public Result<Void> reorderTopics(@RequestBody List<Requests.TopicReorderItem> items) {
        articleService.reorderTopics(items);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        articleService.delete(id);
        return Result.ok();
    }

}
