/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.core;

import cn.hutool.core.collection.CollUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 幂等上下文
 *
 * 作用：
 * - 在一次请求或方法执行过程中，存储临时上下文数据
 * - 支持跨方法、跨组件共享数据，避免参数层层传递
 * - 线程安全（每个线程有自己的 Map，互不干扰）
 *
 * 使用场景：
 * - AOP 切面中记录幂等状态
 * - 日志记录中添加 traceId、method 等信息
 * - 分布式锁中传递锁标识
 *
 * @author zhangjlk
 * @date 2025/10/6 12:51
 */
public final class IdempotentContext {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = new ThreadLocal<>();

    /**
     * 获取当前线程的上下文 Map
     *
     * @return 当前线程的上下文 Map（可能为 null）
     */
    public static Map<String, Object> get() {
        return CONTEXT.get();
    }

    /**
     * 根据 key 获取上下文中的值
     *
     * @param key 键名
     * @return 对应值，不存在返回 null
     */
    public static Object getKey(String key) {
        Map<String, Object> context = get();
        if (CollUtil.isNotEmpty(context)) {
            return context.get(key);
        }
        return null;
    }

    /**
     * 根据 key 获取字符串值
     *
     * @param key 键名
     * @return 字符串值，不存在或为 null 返回 null
     */
    public static String getString(String key) {
        Object actual = getKey(key);
        if (actual != null) {
            return actual.toString();
        }
        return null;
    }

    /**
     * 存入一个键值对到上下文
     *
     * @param key   键名
     * @param value 值
     */
    public static void put(String key, Object value) {
        Map<String, Object> context = get();
        if (CollUtil.isEmpty(context)) {
            context = new HashMap<>(); // 如果没有上下文，创建一个新的上下文
        }

        context.put(key, value);
        putContext(context);
    }

    /**
     * 直接设置整个上下文 Map
     *
     * @param context 要设置的上下文 Map
     */
    public static void putContext(Map<String, Object> context) {
        Map<String, Object> threadContext = CONTEXT.get();
        if (CollUtil.isNotEmpty(threadContext)) {
            threadContext.putAll(context); // 合并已有数据
            return;
        }
        CONTEXT.set(context); // 设置新上下文
    }

    public static void clean() {
        CONTEXT.remove();
    }
}
