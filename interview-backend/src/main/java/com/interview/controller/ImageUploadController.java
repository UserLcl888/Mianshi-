package com.interview.controller;

import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.Result;
import com.interview.service.MarkdownImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * 图片上传：编辑器手动插图用。登录即可使用。
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class ImageUploadController {

    private static final Set<String> ALLOWED_DIR = Set.of("article", "upload", "image", "cover", "banner");

    private final MarkdownImageService markdownImageService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "dir", defaultValue = "image") String dir) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择图片文件");
        }
        if (!ALLOWED_DIR.contains(dir)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "目录参数不合法");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
        try {
            String url = markdownImageService.storeImage(file.getBytes(), ext, dir);
            return Result.ok(Map.of("url", url));
        } catch (IOException e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "图片读取失败，请重试");
        }
    }
}
