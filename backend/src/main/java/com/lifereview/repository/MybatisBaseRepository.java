package com.lifereview.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 数据访问基础扩展：在 MyBatis-Plus {@link BaseMapper} 之上提供与 JPA 风格一致的通用 CRUD 方法，
 * 供各业务表对应的 Mapper 继承；不绑定单张业务表，泛型 {@code T} 由子接口指定实体类型。
 *
 * @param <T> 实体类型，须包含可访问的 {@code Long} 类型 {@code id} 字段
 */
public interface MybatisBaseRepository<T> extends BaseMapper<T> {

    /**
     * 保存实体：主键为空则插入，否则按主键更新。
     *
     * @param entity 待保存实体
     * @return 保存后的实体（与入参同一引用）
     */
    default T save(T entity) {
        Long id = readId(entity);
        if (id == null) {
            insert(entity);
        } else {
            updateById(entity);
        }
        return entity;
    }

    /**
     * 保存实体并“刷新”；当前实现与 {@link #save(Object)} 相同，便于与 JPA 命名习惯对齐。
     *
     * @param entity 待保存实体
     * @return 保存后的实体
     */
    default T saveAndFlush(T entity) {
        return save(entity);
    }

    /**
     * 按主键查询单条记录。
     *
     * @param id 主键
     * @return 存在则封装为 {@link Optional}，否则为空
     */
    default Optional<T> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /**
     * 判断指定主键是否存在对应记录。
     *
     * @param id 主键
     * @return 存在为 {@code true}，否则为 {@code false}
     */
    default boolean existsById(Long id) {
        return selectById(id) != null;
    }

    /**
     * 查询当前实体映射表中的全部记录。
     *
     * @return 全部记录列表，无结果时为空列表
     */
    default List<T> findAll() {
        return selectList(new QueryWrapper<>());
    }

    /**
     * 按主键集合批量查询。
     *
     * @param ids 主键迭代器，其中的 {@code null} 会被忽略
     * @return 匹配到的实体列表；若入参无有效 id 则返回空列表
     */
    default List<T> findAllById(Iterable<Long> ids) {
        List<Long> values = new ArrayList<>();
        for (Long id : ids) {
            if (id != null) {
                values.add(id);
            }
        }
        if (values.isEmpty()) {
            return List.of();
        }
        return selectByIds(values);
    }

    /**
     * 按实体主键删除；若无法读取主键或主键为空则不做任何操作。
     *
     * @param entity 含主键的实体
     */
    default void delete(T entity) {
        Long id = readId(entity);
        if (id != null) {
            deleteById(id);
        }
    }

    /**
     * 通过反射读取实体上名为 {@code id} 的 {@code Long} 类型主键字段值。
     *
     * @param entity 实体对象
     * @return 主键值，字段为 {@code null} 时返回 {@code null}
     */
    private Long readId(T entity) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value == null) {
                return null;
            }
            if (value instanceof Long) {
                return (Long) value;
            }
            return Long.valueOf(String.valueOf(value));
        } catch (Exception ex) {
            throw new IllegalStateException("实体缺少 Long 类型 id 字段: " + entity.getClass().getName(), ex);
        }
    }
}
