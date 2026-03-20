// 包声明：Repository 所在包
package com.lifereview.repository;

import com.lifereview.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/** 用户表 Mapper：按手机号、邮箱查询，校验重复 */
@Mapper
public interface UserRepository extends MybatisBaseRepository<User> {
    @Select("select * from users where phone = #{phone} limit 1")
    User findOneByPhone(String phone);

    @Select("select * from users where email = #{email} limit 1")
    User findOneByEmail(String email);

    @Select("select count(1) from users where phone = #{phone} and id <> #{id}")
    long countByPhoneAndIdNot(String phone, Long id);

    @Select("select count(1) from users where email = #{email} and id <> #{id}")
    long countByEmailAndIdNot(String email, Long id);

    default Optional<User> findByPhone(String phone) {
        return Optional.ofNullable(findOneByPhone(phone));
    }

    default Optional<User> findByEmail(String email) {
        return Optional.ofNullable(findOneByEmail(email));
    }

    default boolean existsByPhoneAndIdNot(String phone, Long id) {
        return countByPhoneAndIdNot(phone, id) > 0;
    }

    default boolean existsByEmailAndIdNot(String email, Long id) {
        return countByEmailAndIdNot(email, id) > 0;
    }
}
