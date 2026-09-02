package com.interview.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * MinIO 客户端。未配置 accessKey/secretKey 时不创建客户端（图片功能关闭）。
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties props;

    @Bean
    public MinioClient minioClient() {
        if (!StringUtils.hasText(props.getAccessKey()) || !StringUtils.hasText(props.getEndpoint())) {
            return null;
        }
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}
