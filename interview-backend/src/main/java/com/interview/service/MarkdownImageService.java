package com.interview.service;

import com.interview.common.BizException;
import com.interview.common.ErrorCode;
import com.interview.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 图片自动处理：
 * 解析 md 中的图片引用，把 base64 图片、外链图片、后端本地 /images/ 图片
 * 自动上传到 MinIO，并把 URL 重写为 MinIO 地址；已是 MinIO 的 URL 跳过。
 * MinIO 未配置（accessKey 为空）时原样返回，不影响站点其它功能。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownImageService {

    private static final Pattern IMG_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)\\s]+)(\\s+\"[^\"]*\")?\\)");
    private static final Pattern BASE64_PATTERN =
            Pattern.compile("^data:image/(png|jpe?g|gif|webp);base64,(.+)$", Pattern.DOTALL);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final Set<String> ALLOWED_DIR = Set.of("article", "upload", "image", "cover", "banner", "avatar");
    private static final Map<String, String> CONTENT_TYPE = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp");

    private final MinioProperties props;

    @Autowired(required = false)
    private MinioClient minioClient;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** 处理整篇 md：把图片 URL 全部重写为 MinIO 地址。 */
    public String processImages(String md, String dir) {
        if (!StringUtils.hasText(md) || minioClient == null || !StringUtils.hasText(props.getPublicBaseUrl())) {
            return md;
        }
        Matcher m = IMG_PATTERN.matcher(md);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String alt = m.group(1);
            String url = m.group(2);
            String newUrl = processOne(url, dir);
            m.appendReplacement(sb, Matcher.quoteReplacement("![" + alt + "](" + newUrl + ")"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String processOne(String url, String dir) {
        if (isMinioUrl(url)) {
            return url;
        }
        try {
            byte[] data;
            String ext;
            if (url.startsWith("data:image/")) {
                Matcher bm = BASE64_PATTERN.matcher(url);
                if (!bm.matches()) {
                    return url;
                }
                ext = normalizeExt(bm.group(1));
                data = Base64.getDecoder().decode(bm.group(2));
            } else if (url.startsWith("http://") || url.startsWith("https://")) {
                byte[] downloaded = download(url);
                if (downloaded == null) {
                    return url;
                }
                ext = extFromUrl(url);
                data = downloaded;
            } else if (url.startsWith("/images/")) {
                String name = url.substring("/images/".length());
                ClassPathResource res = new ClassPathResource("static/images/" + name);
                if (!res.exists()) {
                    return url;
                }
                ext = extFromUrl(name);
                data = res.getInputStream().readAllBytes();
            } else {
                // 相对路径等单文件上传无法解析,保留原样
                return url;
            }
            if (data.length == 0 || data.length > props.getImageMaxSize()) {
                return url;
            }
            return storeImage(data, ext, dir);
        } catch (Exception e) {
            log.warn("图片自动上传失败,保留原链接 url={}", url, e);
            return url;
        }
    }

    /** 单张图片上传（手动插图/封面上传接口复用）。未配置 MinIO 时抛业务异常。 */
    public String storeImage(byte[] data, String ext, String dir) {
        if (minioClient == null || !StringUtils.hasText(props.getPublicBaseUrl())) {
            throw new BizException(ErrorCode.SERVER_ERROR, "图片存储未配置，请联系管理员");
        }
        String normDir = StringUtils.hasText(dir) ? dir.trim() : "image";
        if (!ALLOWED_DIR.contains(normDir)) {
            normDir = "image";
        }
        String normExt = normalizeExt(ext);
        if (!ALLOWED_EXT.contains(normExt)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仅支持 png/jpg/gif/webp 图片");
        }
        if (data.length == 0 || data.length > props.getImageMaxSize()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "图片不能为空且不能超过 "
                    + (props.getImageMaxSize() / 1024 / 1024) + "MB");
        }
        try {
            String object = objectKey(normDir, normExt);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(object)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(CONTENT_TYPE.getOrDefault(normExt, "application/octet-stream"))
                    .build());
            return props.getPublicBaseUrl() + "/" + props.getBucket() + "/" + object;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SERVER_ERROR, "图片上传失败：" + e.getMessage());
        }
    }

    /** 当前是否已启用 MinIO 存储。 */
    public boolean enabled() {
        return minioClient != null && StringUtils.hasText(props.getPublicBaseUrl());
    }

    private boolean isMinioUrl(String url) {
        String prefix = props.getPublicBaseUrl() + "/" + props.getBucket() + "/";
        return url.startsWith(prefix);
    }

    /** 删除某个 MinIO 对象（按完整 URL）。非 MinIO URL 或删除失败均忽略，供替换头像等清理用。 */
    public void removeObjectByUrl(String url) {
        if (!StringUtils.hasText(url) || !isMinioUrl(url)) {
            return;
        }
        try {
            if (minioClient == null) {
                return;
            }
            String prefix = props.getPublicBaseUrl() + "/" + props.getBucket() + "/";
            String object = url.substring(prefix.length());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(object)
                    .build());
            log.info("已移除 MinIO 对象: {}", object);
        } catch (Exception e) {
            log.warn("删除 MinIO 对象失败 url={}", url, e);
        }
    }

    private byte[] download(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (compatible; InterviewBot/1.0)")
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200 || resp.body() == null) {
                return null;
            }
            if (resp.body().length > props.getImageMaxSize()) {
                return null;
            }
            return resp.body();
        } catch (Exception e) {
            log.debug("外链图片下载失败 url={}", url, e);
            return null;
        }
    }

    private String objectKey(String dir, String ext) {
        return dir + "/" + LocalDate.now().format(DAY) + "/" + UUID.randomUUID() + "." + ext;
    }

    private String extFromUrl(String url) {
        String path = url;
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        int slash = path.lastIndexOf('/');
        if (slash >= 0) {
            path = path.substring(slash + 1);
        }
        int dot = path.lastIndexOf('.');
        if (dot >= 0 && dot < path.length() - 1) {
            return normalizeExt(path.substring(dot + 1));
        }
        return "png";
    }

    private String normalizeExt(String ext) {
        if (ext == null) {
            return "png";
        }
        String e = ext.toLowerCase();
        if ("jpeg".equals(e)) {
            return "jpg";
        }
        return e;
    }
}
