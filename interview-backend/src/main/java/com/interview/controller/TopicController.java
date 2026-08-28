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

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final ArticleService articleService;

    /**
     * 专题分享专栏列表：按标题搜索，置顶优先，其次手动排序。
     */
    @GetMapping("/articles")
    public Result<PageResult<VOs.ArticleListItemVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(articleService.topicList(keyword, page, size));
    }
}
