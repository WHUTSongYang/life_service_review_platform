package com.lifereview.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Spring MVC 定制配置。
 * <p>
 * 功能包括：为 {@code /api/**} 注册 {@link LoginInterceptor} 做 JWT 校验；
 * 配置 CORS 满足前后端分离跨域；将 {@code app.upload.public-prefix} 映射到本地
 * {@code app.upload.local-path}，提供上传文件 HTTP 访问。
 * </p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    /** 登录拦截器，统一处理 API 鉴权 */
    private final LoginInterceptor loginInterceptor;

    /** 本地上传文件存储目录，默认 {@code ./uploads} */
    @Value("${app.upload.local-path:./uploads}")
    private String localPath;

    /** 对外暴露的静态资源 URL 前缀，默认 {@code /uploads} */
    @Value("${app.upload.public-prefix:/uploads}")
    private String publicPrefix;

    /**
     * 注册拦截器：仅拦截 {@code /api/**}。
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**");
    }

    /**
     * 配置跨域：允许任意来源、方法、请求头（常见于开发或内网网关场景）。
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("*")
                .allowedOrigins("*")
                .allowedHeaders("*");
    }

    /**
     * 将 {@code publicPrefix/**} 映射到本地磁盘目录，用于访问已上传文件。
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String cleanPrefix = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix; // 规范前缀
        String absolutePath = Path.of(localPath).toAbsolutePath().normalize().toString().replace("\\", "/"); // Windows 路径转 URL 风格
        registry.addResourceHandler(cleanPrefix + "/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
