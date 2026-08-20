package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public Result<VOs.CategoryVO> create(@Valid @RequestBody Requests.CategorySaveDTO dto) {
        return Result.ok(categoryService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<VOs.CategoryVO> update(@PathVariable Long id, @Valid @RequestBody Requests.CategorySaveDTO dto) {
        return Result.ok(categoryService.update(id, dto));
    }

    @PutMapping("/reorder")
    public Result<Void> reorder(@RequestBody List<Requests.CategoryReorderItem> items) {
        categoryService.reorder(items);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }
}
