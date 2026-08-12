package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.Result;
import com.interview.dto.VOs;
import com.interview.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/top-articles")
    public Result<List<VOs.TopArticleVO>> topArticles() {
        return Result.ok(statsService.topArticles(10));
    }

    @GetMapping("/category-stats")
    public Result<List<VOs.CategoryStatsVO>> categoryStats() {
        return Result.ok(statsService.categoryStats());
    }
}
