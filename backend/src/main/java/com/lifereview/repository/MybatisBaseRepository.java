// 包声明：Repository 所在包
package com.lifereview.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MyBatis-Plus BaseMapper 扩展，提供 save/findById/existsById/findAll 等 JPA 风格方法 */
public interface MybatisBaseRepository<T> extends BaseMapper<T> {

    /** id 为空则 insert，否则 updateById */
    default T save(T entity) {
        Long id = readId(entity);
        if (id == null) {
            insert(entity);
        } else {
            updateById(entity);
        }
        return entity;
    }

    default T saveAndFlush(T entity) {
        return save(entity);
    }

    default Optional<T> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    default boolean existsById(Long id) {
        return selectById(id) != null;
    }

    default List<T> findAll() {
        return selectList(new QueryWrapper<>());
    }

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

    default void delete(T entity) {
        Long id = readId(entity);
        if (id != null) {
            deleteById(id);
        }
    }

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
