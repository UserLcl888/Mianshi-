package com.interview.controller;

import com.interview.common.PageResult;
import com.interview.common.Result;
import com.interview.dto.Requests;
import com.interview.dto.VOs;
import com.interview.service.AuthService;
import com.interview.service.UserUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/uploads")
@RequiredArgsConstructor
public class UserUploadController {

    private final UserUploadService userUploadService;
    private final AuthService authService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<VOs.UserUploadListItemVO> create(
            @RequestParam("title") String title,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "groupName", required = false) String groupName,
            @RequestPart("file") MultipartFile file) {
        Requests.UserUploadSaveDTO dto = new Requests.UserUploadSaveDTO();
        dto.setTitle(title);
        dto.setCategoryName(categoryName);
        dto.setGroupName(groupName);
        return Result.ok(userUploadService.create(authService.currentUser().getId(), dto, file));
    }

    @GetMapping
    public Result<PageResult<VOs.UserUploadListItemVO>> myList(
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.ok(userUploadService.myList(authService.currentUser().getId(), page, size));
    }

    @GetMapping("/{id}")
    public Result<VOs.UserUploadDetailVO> detail(@PathVariable("id") Long id) {
        return Result.ok(userUploadService.myDetail(authService.currentUser().getId(), id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        userUploadService.deleteByUser(authService.currentUser().getId(), id);
        return Result.ok();
    }
}
