package com.lifereview.repository;

import com.lifereview.entity.AdminAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 管理员账号数据访问：对应表 {@code admin_accounts}，支持按用户名查询与基础 CRUD。
 */
@Mapper
public interface AdminAccountRepository extends MybatisBaseRepository<AdminAccount> {
    /**
     * 按用户名查询一条管理员记录（至多一条）。
     *
     * @param username 登录用户名
     * @return 匹配的管理员实体，不存在时为 {@code null}
     */
    @Select("select * from admin_accounts where username = #{username} limit 1")
    AdminAccount findOneByUsername(String username);

    /**
     * 按用户名查询管理员，结果用 {@link Optional} 表示是否存在。
     *
     * @param username 登录用户名
     * @return 非空则包含实体，否则为空 {@link Optional}
     */
    default Optional<AdminAccount> findByUsername(String username) {
        return Optional.ofNullable(findOneByUsername(username));
    }
}
