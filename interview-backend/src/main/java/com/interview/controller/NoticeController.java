package com.interview.controller;

import com.interview.common.Result;
import com.interview.entity.Notice;
import com.interview.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公告（公开，前台滚动条读取）。 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public Result<List<Notice>> list() {
        return Result.ok(noticeService.listPublic());
    }
}
