package com.interview.controller;

import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AccessService;
import com.interview.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/access")
@RequiredArgsConstructor
public class AccessController {

    private final AccessService accessService;
    private final AuthService authService;

    @GetMapping("/locked-categories")
    public Result<List<VOs.LockedArticleVO>> lockedCategories() {
        return Result.ok(accessService.lockedCategories(authService.currentUser().getId()));
    }

    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody Requests.AccessApplyDTO dto) {
        accessService.apply(authService.currentUser().getId(), dto);
        return Result.ok();
    }

    @GetMapping("/my")
    public Result<List<VOs.AccessApplyVO>> myList() {
        return Result.ok(accessService.myList(authService.currentUser().getId()));
    }

    @GetMapping("/status")
    public Result<VOs.AccessStatusVO> status(@RequestParam("slug") String slug) {
        return Result.ok(accessService.statusBySlug(authService.currentUser().getId(), slug));
    }
}
