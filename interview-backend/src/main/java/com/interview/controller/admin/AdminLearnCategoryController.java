package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.LearnCategoryService;
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
@RequestMapping("/api/admin/learn-categories")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminLearnCategoryController {

    private final LearnCategoryService learnCategoryService;

    @GetMapping
    public Result<List<VOs.LearnCategoryVO>> list() {
        return Result.ok(learnCategoryService.list());
    }

    @PostMapping
    public Result<VOs.LearnCategoryVO> create(@Valid @RequestBody Requests.LearnCategorySaveDTO dto) {
        return Result.ok(learnCategoryService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<VOs.LearnCategoryVO> update(@PathVariable("id") Long id,
                                              @Valid @RequestBody Requests.LearnCategorySaveDTO dto) {
        return Result.ok(learnCategoryService.update(id, dto));
    }

    @PutMapping("/reorder")
    public Result<Void> reorder(@RequestBody List<Requests.LearnCategoryReorderItem> items) {
        learnCategoryService.reorder(items);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        learnCategoryService.delete(id);
        return Result.ok();
    }
}
