package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    /**
     * 每日一句：按日期轮换（当天固定，次日切换）。
     */
    @GetMapping("/quote")
    public Result<Map<String, String>> quote() {
        return Result.ok(homeService.quote());
    }

    /**
     * 首页公开数据：站点统计 + 热门文档 + 热门标签。
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(homeService.overview());
    }
}
