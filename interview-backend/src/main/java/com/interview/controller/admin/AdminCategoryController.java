package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.entity.Category;
import com.interview.service.AdminLogService;
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

@RestController
@RequestMapping("/api/admin/categories")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final AdminLogService adminLogService;

    @PostMapping
    public Result<Category> create(@Valid @RequestBody Requests.CategorySaveDTO dto) {
        Category category = categoryService.create(dto);
        adminLogService.write("CATEGORY_CREATE", "CATEGORY", category.getId(), "新增分类 " + category.getName());
        return Result.ok(category);
    }

    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @Valid @RequestBody Requests.CategorySaveDTO dto) {
        Category category = categoryService.update(id, dto);
        adminLogService.write("CATEGORY_UPDATE", "CATEGORY", id, "编辑分类 " + category.getName());
        return Result.ok(category);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        adminLogService.write("CATEGORY_DELETE", "CATEGORY", id, "删除分类");
        return Result.ok();
    }
}
