package com.interview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 统一的 Markdown 内容加工入口：
 * 1) 把 md 中的图片自动上传 MinIO 并重写为 MinIO URL（未配置 MinIO 时原样返回）；
 * 2) 再把处理后的 md 渲染为已消毒的 HTML。
 * 供文章、用户投稿等复用，避免各自重复「processImages + render + 写两个字段」。
 */
@Service
@RequiredArgsConstructor
public class ContentRenderService {

    private final MarkdownImageService markdownImageService;
    private final MarkdownService markdownService;

    /** 处理 Markdown 正文，返回供落库的 contentMd 与 contentHtml。 */
    public RenderedContent render(String rawMd, String imageDir) {
        String contentMd = markdownImageService.processImages(rawMd == null ? "" : rawMd, imageDir);
        return new RenderedContent(contentMd, markdownService.render(contentMd));
    }

    public record RenderedContent(String contentMd, String contentHtml) {
    }
}
