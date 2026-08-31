package com.interview.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.common.Result;
import com.interview.service.MarkdownImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端封面上传：先落本地 uploads/covers/ 目录，通过 /images/covers/** 访问；
 * MinIO 接入后仅需替换存储实现，返回 URL 规则保持不变。
 */
@RestController
@RequestMapping("/api/admin/upload")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
@Slf4j
public class AdminCoverController {

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "webp");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private final MarkdownImageService markdownImageService;

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadCover(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择封面图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ErrorCode.PARAM_ERROR, "封面图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().trim();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持 png/jpg/jpeg/webp 格式");
        }
        // MinIO 已配置：封面直接存 MinIO，返回公开 URL
        if (markdownImageService.enabled()) {
            try {
                String url = markdownImageService.storeImage(file.getBytes(), ext, "cover");
                return Result.ok(Map.of("url", url));
            } catch (IOException e) {
                throw new BizException(ErrorCode.SERVER_ERROR, "封面读取失败，请重试");
            }
        }
        // MinIO 未配置：回退本地 uploads/covers/ 存储
        String day = LocalDate.now().format(DAY);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadDir).resolve("covers").resolve(day);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            log.error("封面本地保存失败, uploadDir={}, day={}", uploadDir, day, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "封面上传失败，请重试");
        }
        return Result.ok(Map.of("url", "/images/covers/" + day + "/" + fileName));
    }
}
