package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AccessService;
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
@RequestMapping("/api/admin/access")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminAccessController {

    private final AccessService accessService;

    @GetMapping
    public Result<PageResult<VOs.AccessApplyVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(accessService.adminList(keyword, status, scope, page, size));
    }

    @GetMapping("/{id}")
    public Result<VOs.AccessApplyVO> detail(@PathVariable("id") Long id) {
        return Result.ok(accessService.adminDetail(id));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable("id") Long id, @RequestBody(required = false) Requests.AccessReviewDTO dto) {
        accessService.approve(id, dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable("id") Long id, @RequestBody(required = false) Requests.AccessReviewDTO dto) {
        accessService.reject(id, dto == null ? null : dto.getRemark());
        return Result.ok();
    }

    @PutMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable("id") Long id, @Valid @RequestBody Requests.AdminReplyDTO dto) {
        accessService.reply(id, dto.getContent());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        accessService.delete(id);
        return Result.ok();
    }
}
