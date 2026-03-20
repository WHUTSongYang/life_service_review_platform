// 包声明：生活服务点评平台主包
package com.lifereview;

// MyBatis 扫描注解，用于自动发现 Mapper 接口
import org.mybatis.spring.annotation.MapperScan;
// Spring Boot 启动入口
import org.springframework.boot.SpringApplication;
// Spring Boot 自动配置注解
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 生活服务点评平台启动类。MapperScan 扫描 repository 包下的 MyBatis Mapper 接口，实现数据访问层自动装配 */
@SpringBootApplication
@MapperScan("com.lifereview.repository")
public class LifeReviewApplication {
    // 程序入口：启动 Spring Boot 应用
    public static void main(String[] args) {
        // 运行应用，传入主类与命令行参数
        SpringApplication.run(LifeReviewApplication.class, args);
    }
}
