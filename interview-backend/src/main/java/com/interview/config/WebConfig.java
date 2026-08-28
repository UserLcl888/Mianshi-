package com.interview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 封面上传目录通过 /images/covers/** 访问，与前端 /images 代理保持一致。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String covers = Paths.get(uploadDir).resolve("covers").toAbsolutePath().normalize().toUri().toString();
        if (!covers.endsWith("/")) {
            covers += "/";
        }
        registry.addResourceHandler("/images/covers/**").addResourceLocations(covers);
    }
}
