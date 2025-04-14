package com.itheima.fve.fve.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
// *** 替换为你的 User 实体类的实际路径 ***
import com.itheima.fve.fve.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class MyMetaObjecthandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]...");
        Object originalObject = metaObject.getOriginalObject();
        LocalDateTime now = LocalDateTime.now();
        long threadId = Thread.currentThread().getId();
        log.info("当前线程 ID: {}", threadId);
        log.debug("MetaObject before fill for type [{}]: {}", originalObject.getClass().getName(), metaObject.toString());

        // *** 关键：根据实体类型判断 ***
        // *** 请确保这里的 User 类路径是正确的 ***
        if (originalObject instanceof User) {
            log.info("检测到正在插入 User 实体，执行用户注册填充逻辑...");
            // 仅填充 user 表实际存在的字段
            trySetFieldValByName("createTime", now, metaObject);
            // 假设你已经给 user 表添加了 update_time, create_user, update_user
            trySetFieldValByName("updateTime", now, metaObject);
            // 对于 createUser 和 updateUser，在注册时不设置，允许数据库插入 null 或默认值
            log.warn("用户注册场景，createUser 和 updateUser 将为 null。");

        } else {
            log.info("检测到正在插入非 User 实体 (例如商品)，执行标准填充逻辑...");
            trySetFieldValByName("createTime", now, metaObject);
            trySetFieldValByName("updateTime", now, metaObject);

            Long currentUserId = BaseContext.getCurrentId();
            log.info("从 BaseContext 获取到的当前用户 ID: {}", currentUserId);

            if (currentUserId != null) {
                trySetFieldValByName("createUser", currentUserId, metaObject);
                trySetFieldValByName("updateUser", currentUserId, metaObject);
                log.info("成功为标准实体设置 createUser 和 updateUser 为: {}", currentUserId);
            } else {
                log.error("无法从 BaseContext 获取用户 ID，标准实体（非User）无法填充 createUser 和 updateUser！");
                throw new IllegalStateException("无法获取当前操作用户ID，无法执行标准实体的插入！"); // 保留对其他实体的严格要求
            }
        }
        log.debug("MetaObject after fill: {}", metaObject.getOriginalObject());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充[update]...");
        Object originalObject = metaObject.getOriginalObject();
        LocalDateTime now = LocalDateTime.now();
        long threadId = Thread.currentThread().getId();
        log.info("当前线程 ID: {}", threadId);
        log.debug("MetaObject before fill for type [{}]: {}", originalObject.getClass().getName(), metaObject.toString());

        // 填充 updateTime (假设所有要更新的表都有)
        trySetFieldValByName("updateTime", now, metaObject);

        Long currentUserId = BaseContext.getCurrentId();
        log.info("从 BaseContext 获取到的当前用户 ID: {}", currentUserId);

        if (currentUserId != null) {
            // 填充 updateUser (假设所有要更新的表都有)
            trySetFieldValByName("updateUser", currentUserId, metaObject);
            log.info("成功设置 updateUser 为: {}", currentUserId);
        } else {
            // 根据类型决定是否抛异常
            // *** 请确保这里的 User 类路径是正确的 ***
            if (originalObject instanceof User) {
                log.warn("正在更新 User 实体，但无法从 BaseContext 获取用户 ID。updateUser 未设置。");
                // 如果不允许匿名更新 User，在这里抛异常
                // throw new IllegalStateException("无法获取当前操作用户ID，无法执行 User 的更新！");
            } else {
                log.warn("无法从 BaseContext 获取用户 ID，标准实体（非User）updateUser 未设置!");
                throw new IllegalStateException("无法获取当前操作用户ID，无法执行标准实体的更新！"); // 保留对其他实体的严格要求
            }
        }
        log.debug("MetaObject after fill: {}", metaObject.getOriginalObject());
    }

    private void trySetFieldValByName(String fieldName, Object fieldVal, MetaObject metaObject) {
        if (fieldVal != null && metaObject.hasSetter(fieldName)) {
            if (metaObject.hasSetter(fieldName)) {
                try {
                    metaObject.setValue(fieldName, fieldVal);
                } catch (Exception e) {
                    log.error("自动填充字段 '{}' 时发生错误: {}", fieldName, e.getMessage(), e);
                }
            } else {
                log.warn("实体类 '{}' 不存在字段 '{}' 的 setter 方法，跳过填充。", metaObject.getOriginalObject().getClass().getSimpleName(), fieldName);
            }
        }
    }
}