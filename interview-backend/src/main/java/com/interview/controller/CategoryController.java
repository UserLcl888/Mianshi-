package com.interview.controller;

import com.interview.common.Result;
import com.interview.dto.VOs;
import com.interview.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<VOs.CategoryVO>> tree() {
        return Result.ok(categoryService.tree());
    }
}
