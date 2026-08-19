package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.TagService;
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
@RequestMapping("/api/admin/tags")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    @GetMapping
    public Result<List<VOs.TagVO>> list() {
        return Result.ok(tagService.listAll());
    }

    @PostMapping
    public Result<VOs.TagVO> create(@Valid @RequestBody Requests.TagSaveDTO dto) {
        return Result.ok(tagService.create(dto.getName()));
    }

    @PutMapping("/{id}")
    public Result<VOs.TagVO> update(@PathVariable Long id, @Valid @RequestBody Requests.TagSaveDTO dto) {
        return Result.ok(tagService.update(id, dto.getName()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }
}
