package com.lifereview.repository;

import com.lifereview.entity.AdminAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/** 管理员账号表 Mapper */
@Mapper
public interface AdminAccountRepository extends MybatisBaseRepository<AdminAccount> {
    @Select("select * from admin_accounts where username = #{username} limit 1")
    AdminAccount findOneByUsername(String username);

    default Optional<AdminAccount> findByUsername(String username) {
        return Optional.ofNullable(findOneByUsername(username));
    }
}
