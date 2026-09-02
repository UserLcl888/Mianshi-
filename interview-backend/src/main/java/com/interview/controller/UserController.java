package com.interview.controller;

import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @GetMapping("/profile")
    public Result<VOs.UserVO> profile() {
        return Result.ok(authService.profile());
    }

    @PutMapping("/profile")
    public Result<VOs.UserVO> updateProfile(@Valid @RequestBody Requests.UpdateNicknameDTO dto) {
        return Result.ok(authService.updateNickname(dto));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody Requests.ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.ok();
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<VOs.UserVO> updateAvatar(@RequestPart("file") MultipartFile file) {
        return Result.ok(authService.updateAvatar(file));
    }
}
