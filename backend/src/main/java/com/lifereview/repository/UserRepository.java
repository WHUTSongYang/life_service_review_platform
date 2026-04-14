package com.lifereview.repository;

import com.lifereview.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 平台用户数据访问：对应表 {@code users}，用于注册登录、资料修改时的手机号/邮箱唯一性校验与按条件查询。
 */
@Mapper
public interface UserRepository extends MybatisBaseRepository<User> {
    /**
     * 按手机号查询单条用户。
     *
     * @param phone 手机号
     * @return 匹配的用户实体，不存在时为 {@code null}
     */
    @Select("select * from users where phone = #{phone} limit 1")
    User findOneByPhone(String phone);

    /**
     * 按邮箱查询单条用户。
     *
     * @param email 邮箱
     * @return 匹配的用户实体，不存在时为 {@code null}
     */
    @Select("select * from users where email = #{email} limit 1")
    User findOneByEmail(String email);

    /**
     * 统计指定手机号被「非当前用户 id」占用的记录数，用于更新资料时排除自身。
     *
     * @param phone 手机号
     * @param id    当前用户主键，统计时排除该 id
     * @return 满足条件的行数
     */
    @Select("select count(1) from users where phone = #{phone} and id <> #{id}")
    long countByPhoneAndIdNot(String phone, Long id);

    /**
     * 统计指定邮箱被「非当前用户 id」占用的记录数。
     *
     * @param email 邮箱
     * @param id    当前用户主键，统计时排除该 id
     * @return 满足条件的行数
     */
    @Select("select count(1) from users where email = #{email} and id <> #{id}")
    long countByEmailAndIdNot(String email, Long id);

    /**
     * 按手机号查询用户，封装为 {@link Optional}。
     *
     * @param phone 手机号
     * @return 存在则非空，否则为空
     */
    default Optional<User> findByPhone(String phone) {
        return Optional.ofNullable(findOneByPhone(phone));
    }

    /**
     * 按邮箱查询用户，封装为 {@link Optional}。
     *
     * @param email 邮箱
     * @return 存在则非空，否则为空
     */
    default Optional<User> findByEmail(String email) {
        return Optional.ofNullable(findOneByEmail(email));
    }

    /**
     * 判断手机号是否已被除指定 id 外的其他用户占用。
     *
     * @param phone 手机号
     * @param id    当前用户 id
     * @return 已被占用为 {@code true}
     */
    default boolean existsByPhoneAndIdNot(String phone, Long id) {
        return countByPhoneAndIdNot(phone, id) > 0;
    }

    /**
     * 判断邮箱是否已被除指定 id 外的其他用户占用。
     *
     * @param email 邮箱
     * @param id    当前用户 id
     * @return 已被占用为 {@code true}
     */
    default boolean existsByEmailAndIdNot(String email, Long id) {
        return countByEmailAndIdNot(email, id) > 0;
    }
}
