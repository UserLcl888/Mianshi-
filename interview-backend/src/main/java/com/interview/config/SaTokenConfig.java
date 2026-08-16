package com.interview.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig {

    @Bean
    public SaInterceptor saInterceptor() {
        return new SaInterceptor(handle -> StpUtil.checkLogin());
    }

    @Bean
    public WebMvcConfigurer saWebMvcConfigurer(SaInterceptor saInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(saInterceptor)
                        .addPathPatterns("/api/**")
                        .excludePathPatterns(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/code/email",
                                "/api/auth/login/code",
                                "/api/auth/reset-password",
                                "/api/home/**",
                                "/api/categories/tree");
            }
        };
    }
}
