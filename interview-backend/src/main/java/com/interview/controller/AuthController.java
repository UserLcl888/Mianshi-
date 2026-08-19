package com.interview.controller;

import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AuthService;
import com.interview.service.EmailCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailCodeService emailCodeService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody Requests.RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<VOs.LoginResultVO> login(@Valid @RequestBody Requests.LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/code/email")
    public Result<Map<String, String>> sendEmailCode(@Valid @RequestBody Requests.SendCodeDTO dto) {
        return Result.ok(emailCodeService.send(dto.getEmail(), dto.getScene()));
    }

    @PostMapping("/login/code")
    public Result<VOs.LoginResultVO> loginByCode(@Valid @RequestBody Requests.LoginByCodeDTO dto) {
        return Result.ok(authService.loginByCode(dto));
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody Requests.ResetByCodeDTO dto) {
        authService.resetPasswordByCode(dto);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }
}
