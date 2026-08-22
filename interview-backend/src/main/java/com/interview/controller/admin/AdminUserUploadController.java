package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.UserUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/uploads")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminUserUploadController {

    private final UserUploadService userUploadService;

    @GetMapping
    public Result<PageResult<VOs.UserUploadListItemVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(userUploadService.adminList(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<VOs.UserUploadDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(userUploadService.adminDetail(id));
    }

    @PutMapping("/{id}/reply")
    public Result<VOs.UserUploadDetailVO> reply(@PathVariable("id") Long id,
                                                @Valid @RequestBody Requests.AdminReplyDTO dto) {
        return Result.ok(userUploadService.reply(id, dto.getContent()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        userUploadService.deleteByAdmin(id);
        return Result.ok();
    }
}
