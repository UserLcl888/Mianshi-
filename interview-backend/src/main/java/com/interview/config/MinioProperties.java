package com.interview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 配置。accessKey/secretKey 未配置时图片功能自动禁用，不影响站点其它功能。
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    /** MinIO 服务地址，如 http://127.0.0.1:9000 */
    private String endpoint = "";
    private String accessKey = "";
    private String secretKey = "";
    /** 桶名 */
    private String bucket = "interview-images";
    /** 图片对外访问前缀（不含桶名），如 http://103.236.54.34:9000 */
    private String publicBaseUrl = "";
    /** 单张图片大小上限（字节），默认 10MB */
    private long imageMaxSize = 10L * 1024 * 1024;
}
