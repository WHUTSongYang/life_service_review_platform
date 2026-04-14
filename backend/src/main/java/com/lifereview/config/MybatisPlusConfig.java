package com.lifereview.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置。
 * <p>
 * 注册分页插件，使 Mapper 中使用 {@code Page} 进行分页查询时自动拼接 MySQL 分页 SQL。
 * </p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 构建 MyBatis-Plus 拦截器链，并加入 MySQL 分页内部拦截器。
     *
     * @return 已配置分页能力的 {@link MybatisPlusInterceptor}
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // MySQL 方言分页
        return interceptor;
    }
}
