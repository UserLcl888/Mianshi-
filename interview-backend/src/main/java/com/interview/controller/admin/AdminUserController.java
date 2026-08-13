package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AdminLogService;
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
    private final AdminLogService adminLogService;

    @GetMapping
    public Result<PageResult<VOs.UserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(adminUserService.list(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public Result<VOs.UserVO> get(@PathVariable Long id) {
        return Result.ok(adminUserService.get(id));
    }

    @PostMapping
    public Result<VOs.UserVO> create(@Valid @RequestBody Requests.UserCreateDTO dto) {
        VOs.UserVO vo = adminUserService.create(dto);
        adminLogService.write("USER_CREATE", "USER", vo.getId(), "创建用户 " + vo.getNickname());
        return Result.ok(vo);
    }

    @PutMapping("/{id}")
    public Result<VOs.UserVO> update(@PathVariable Long id, @RequestBody Requests.UserUpdateDTO dto) {
        VOs.UserVO vo = adminUserService.update(id, dto);
        adminLogService.write("USER_UPDATE", "USER", id, "编辑用户 " + vo.getNickname());
        return Result.ok(vo);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody Requests.StatusDTO dto) {
        adminUserService.updateStatus(id, dto.getStatus());
        adminLogService.write(dto.getStatus() == 0 ? "USER_DISABLE" : "USER_ENABLE", "USER", id,
                dto.getStatus() == 0 ? "禁用用户" : "启用用户");
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody Requests.ResetPasswordDTO dto) {
        adminUserService.resetPassword(id, dto.getNewPassword());
        adminLogService.write("USER_RESET_PASSWORD", "USER", id, "重置用户密码");
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        adminLogService.write("USER_DELETE", "USER", id, "删除用户");
        return Result.ok();
    }
}
