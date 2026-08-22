package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AdminUserService;
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
@RequestMapping("/api/admin/users")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<PageResult<VOs.UserVO>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(adminUserService.list(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<VOs.UserVO> get(@PathVariable("id") Long id) {
        return Result.ok(adminUserService.get(id));
    }

    @PostMapping
    public Result<VOs.UserVO> create(@Valid @RequestBody Requests.UserCreateDTO dto) {
        return Result.ok(adminUserService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<VOs.UserVO> update(@PathVariable("id") Long id, @RequestBody Requests.UserUpdateDTO dto) {
        return Result.ok(adminUserService.update(id, dto));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable("id") Long id, @Valid @RequestBody Requests.StatusDTO dto) {
        adminUserService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable("id") Long id, @Valid @RequestBody Requests.ResetPasswordDTO dto) {
        adminUserService.resetPassword(id, dto.getNewPassword());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        adminUserService.delete(id);
        return Result.ok();
    }
}
