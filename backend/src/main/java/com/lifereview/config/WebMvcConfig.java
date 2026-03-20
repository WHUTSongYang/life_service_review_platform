// 包声明：配置类所在包
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
 * Web MVC 配置。
 * 拦截器：为 /api/** 注册 LoginInterceptor，校验 JWT。
 * CORS：允许跨域，开发环境全放行。
 * 静态资源：将 /uploads/** 映射到 app.upload.local-path 本地目录。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    // 登录拦截器，用于 JWT 校验
    private final LoginInterceptor loginInterceptor;

    /** 本地文件上传存储路径，默认 ./uploads */
    @Value("${app.upload.local-path:./uploads}")
    private String localPath;

    /** 静态资源访问 URL 前缀，默认 /uploads */
    @Value("${app.upload.public-prefix:/uploads}")
    private String publicPrefix;

    /** 为 /api/** 路径注册登录拦截器 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加拦截器并指定拦截路径
        registry.addInterceptor(loginInterceptor).addPathPatterns("/api/**");
    }

    /** 允许跨域，开发环境全放行 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 对所有路径启用 CORS
        registry.addMapping("/**")
                // 允许所有 HTTP 方法
                .allowedMethods("*")
                // 允许所有来源
                .allowedOrigins("*")
                // 允许所有请求头
                .allowedHeaders("*");
    }

    /** 将 /uploads/** 映射到本地文件目录，用于图片等静态资源访问 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 确保前缀以 / 开头
        String cleanPrefix = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
        // 转为绝对路径并统一为 / 分隔符
        String absolutePath = Path.of(localPath).toAbsolutePath().normalize().toString().replace("\\", "/");
        // 注册 URL 路径与本地目录的映射
        registry.addResourceHandler(cleanPrefix + "/**")
                .addResourceLocations("file:" + absolutePath + "/");
    }
}
