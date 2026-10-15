package com.visualbuilder.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.visualbuilder.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createdBy", () -> userId, Long.class);
            this.strictInsertFill(metaObject, "updatedBy", () -> userId, Long.class);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedTime", LocalDateTime::now, LocalDateTime.class);
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updatedBy", () -> userId, Long.class);
        }
    }
}
