package com.lifereview.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 通用时间字段自动填充处理器。
 * <p>
 * 约定实体字段命名为 createdAt / updatedAt：
 * 插入时自动写入 createdAt、updatedAt；更新时自动刷新 updatedAt。
 * </p>
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充创建/更新时间。
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        fillIfNull(metaObject, "createdAt", now);
        fillIfNull(metaObject, "updatedAt", now);
    }

    /**
     * 更新时自动刷新更新时间。
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updatedAt")) {
            setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
        }
    }

    private void fillIfNull(MetaObject metaObject, String fieldName, LocalDateTime value) {
        if (!metaObject.hasSetter(fieldName)) {
            return;
        }
        if (getFieldValByName(fieldName, metaObject) == null) {
            setFieldValByName(fieldName, value, metaObject);
        }
    }
}
