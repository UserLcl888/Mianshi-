package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.entity.Article;
import com.interview.service.AdminLogService;
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

@RestController
@RequestMapping("/api/admin/articles")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;
    private final AdminLogService adminLogService;

    @GetMapping
    public Result<PageResult<VOs.ArticleListItemVO>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(articleService.adminList(categoryId, difficulty, page, size));
    }

    @PostMapping
    public Result<Article> create(@Valid @RequestBody Requests.ArticleSaveDTO dto) {
        Article article = articleService.create(dto);
        adminLogService.write("ARTICLE_CREATE", "ARTICLE", article.getId(), "新增题目：" + article.getTitle());
        return Result.ok(article);
    }

    @PutMapping("/{id}")
    public Result<Article> update(@PathVariable Long id, @Valid @RequestBody Requests.ArticleSaveDTO dto) {
        Article article = articleService.update(id, dto);
        adminLogService.write("ARTICLE_UPDATE", "ARTICLE", id, "编辑题目：" + article.getTitle());
        return Result.ok(article);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        adminLogService.write("ARTICLE_DELETE", "ARTICLE", id, "删除题目");
        return Result.ok();
    }

}
