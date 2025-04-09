package com.itheima.fve.fve.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MyMetaObjecthandler implements MetaObjectHandler {

    /**
     * 插入操作，自动填充
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]...");
        log.debug("MetaObject before fill: {}", metaObject.toString()); // 使用 debug 级别

        long threadId = Thread.currentThread().getId();
        log.info("当前线程 ID: {}", threadId);

        // 填充创建时间和更新时间
        trySetFieldValByName("createTime", LocalDateTime.now(), metaObject);
        trySetFieldValByName("updateTime", LocalDateTime.now(), metaObject);

        // 填充创建人和更新人
        Long currentUserId = BaseContext.getCurrentId(); // 获取ID
        log.info("从 BaseContext 获取到的当前用户 ID: {}", currentUserId); // **打印获取到的ID**

        if (currentUserId != null) {
            trySetFieldValByName("createUser", currentUserId, metaObject);
            trySetFieldValByName("updateUser", currentUserId, metaObject);
            log.info("成功设置 createUser 和 updateUser 为: {}", currentUserId);
        } else {
            log.warn("无法从 BaseContext 获取用户 ID，createUser 和 updateUser 未设置!");
            // 如果业务强制要求创建人不能为空，这里可以抛出异常阻止插入
             throw new IllegalStateException("无法获取当前操作用户ID，无法执行插入！");
        }
        log.debug("MetaObject after fill: {}", metaObject.getOriginalObject()); // 打印填充后状态
    }

    /**
     * 更新操作，自动填充
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]...");
        log.debug("MetaObject before fill: {}", metaObject.toString());

        long threadId = Thread.currentThread().getId();
        log.info("当前线程 ID: {}", threadId);

        // 填充更新时间
        trySetFieldValByName("updateTime", LocalDateTime.now(), metaObject);

        // 填充更新人
        Long currentUserId = BaseContext.getCurrentId();
        log.info("从 BaseContext 获取到的当前用户 ID: {}", currentUserId);

        if (currentUserId != null) {
            trySetFieldValByName("updateUser", currentUserId, metaObject);
            log.info("成功设置 updateUser 为: {}", currentUserId);
        } else {
            log.warn("无法从 BaseContext 获取用户 ID，updateUser 未设置!");
            // 抛异常
             throw new IllegalStateException("无法获取当前操作用户ID，无法执行更新！");
        }
        log.debug("MetaObject after fill: {}", metaObject.getOriginalObject());
    }

    /**
     * 尝试设置字段值，避免因字段不存在或类型不匹配抛出异常
     */
    private void trySetFieldValByName(String fieldName, Object fieldVal, MetaObject metaObject) {
        if (fieldVal != null && metaObject.hasSetter(fieldName)) {
            try {
                metaObject.setValue(fieldName, fieldVal);
            } catch (Exception e) {
                log.error("自动填充字段 '{}' 时发生错误: {}", fieldName, e.getMessage());
            }
        }
    }


}