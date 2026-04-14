package com.lifereview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 生活服务点评平台 Spring Boot 启动入口。
 * <p>
 * 负责引导 Spring 容器启动；通过 {@link MapperScan} 扫描 {@code com.lifereview.repository} 包，
 * 自动注册 MyBatis Mapper 接口，完成数据访问层装配。
 * @EnableScheduling开启了定时任务能力
 * </p>
 */
@SpringBootApplication
@MapperScan("com.lifereview.repository")
@EnableScheduling
public class LifeReviewApplication {

    /**
     * 应用程序主入口：启动内嵌 Web 容器并加载 Spring 应用上下文。
     *
     * @param args 命令行参数，透传给 Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(LifeReviewApplication.class, args); // 启动 Spring Boot 应用
    }
}
