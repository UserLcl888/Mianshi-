package com.interview.controller;

import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/profile")
    public Result<VOs.UserVO> profile() {
        return Result.ok(authService.profile());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody Requests.ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.ok();
    }
}
