package com.itheima.fve.fve.common;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于ThreadLocal封装工具类，用户保存和获取当前登录用户id
 */
@Slf4j
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID (由Filter调用)
     * @param id
     */
    public static void setCurrentId(Long id){
        log.info("线程 {} - BaseContext 设置 ID: {}", Thread.currentThread().getId(), id);
        threadLocal.set(id);
    }

    /**
     * 获取当前线程的用户ID (由MetaObjectHandler等调用)
     * @return 如果当前线程已设置ID则返回ID，否则返回null
     */
    public static Long getCurrentId(){
        Long id = threadLocal.get();
        log.info("线程 {} - BaseContext 获取 ID: {}", Thread.currentThread().getId(), id);
        return id;
    }

    /**
     * 移除当前线程的值 (必须在请求处理完成后调用)
     */
    public static void removeCurrentId() {
        log.info("线程 {} - BaseContext 移除 ID", Thread.currentThread().getId());
        threadLocal.remove();
    }
}