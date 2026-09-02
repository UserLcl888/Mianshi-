package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.entity.Notice;
import com.interview.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notices")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public Result<List<Notice>> list() {
        return Result.ok(noticeService.adminList());
    }

    @PostMapping
    public Result<Notice> create(@Valid @RequestBody Requests.NoticeSaveDTO dto) {
        return Result.ok(noticeService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<Notice> update(@PathVariable("id") Long id, @Valid @RequestBody Requests.NoticeSaveDTO dto) {
        return Result.ok(noticeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        noticeService.delete(id);
        return Result.ok();
    }
}
