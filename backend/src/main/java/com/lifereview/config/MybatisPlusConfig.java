// 包声明：配置类所在包
package com.lifereview.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MyBatis-Plus 配置：启用 MySQL 分页插件，支持 Page 分页查询 */
@Configuration
public class MybatisPlusConfig {

    /** 分页拦截器 Bean，拦截 MyBatis 查询并自动拼接 LIMIT 子句 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建 MyBatis-Plus 拦截器链
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加 MySQL 分页内部拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
