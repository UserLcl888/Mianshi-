package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.entity.Tag;
import com.interview.mapper.TagMapper;
import com.interview.mapper.ArticleTagMapper;
import com.interview.service.AdminLogService;
import com.interview.service.ContentCacheService;
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
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final AdminLogService adminLogService;
    private final ContentCacheService contentCacheService;

    @GetMapping
    public Result<List<Tag>> list() {
        return Result.ok(tagService.listAll());
    }

    @PostMapping
    public Result<Tag> create(@Valid @RequestBody Requests.TagSaveDTO dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tagMapper.insert(tag);
        contentCacheService.bump();
        adminLogService.write("TAG_CREATE", "TAG", tag.getId(), "新增标签 " + tag.getName());
        return Result.ok(tag);
    }

    @PutMapping("/{id}")
    public Result<Tag> update(@PathVariable Long id, @Valid @RequestBody Requests.TagSaveDTO dto) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        tag.setName(dto.getName());
        tagMapper.updateById(tag);
        contentCacheService.bump();
        adminLogService.write("TAG_UPDATE", "TAG", id, "编辑标签 " + tag.getName());
        return Result.ok(tag);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleTagMapper.deleteByTagId(id);
        tagMapper.deleteById(id);
        contentCacheService.bump();
        adminLogService.write("TAG_DELETE", "TAG", id, "删除标签");
        return Result.ok();
    }
}
