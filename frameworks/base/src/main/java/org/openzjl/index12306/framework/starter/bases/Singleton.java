/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.bases;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 单例对象容器
 * @author zhangjlk
 * @date 2025/9/6 21:51
 */
// 控制这个无参构造函数的访问权限
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Singleton {

    private static final ConcurrentHashMap<String, Object> SINGLE_OBJECT_POOL = new ConcurrentHashMap<>();

    /**
     * 根据key获取单例对象
     * @param key 单例对象主键
     * @return 单例对象
     * @param <T> 单例对象属性
     */
    public static <T> T get(String key) {
        Object result = SINGLE_OBJECT_POOL.get(key);
        return result == null ? null : (T) result;
    }

    /**
     * 从全局单例对象池中获取指定键对应的对象。
     * 若对象不存在，则通过 supplier 创建并缓存（仅当创建结果非 null 时）。
     *
     * @param <T>      对象的泛型类型
     * @param key      对象的唯一标识键
     * @param supplier 用于延迟创建对象的无参生产者函数
     * @return 缓存中已存在的对象，或新创建的对象；若创建结果为 null 则返回 null
     */
    public static <T> T get(String key, Supplier<T> supplier) {
        // 从全局缓存中尝试获取已存在的对象
        Object result = SINGLE_OBJECT_POOL.get(key);

        // 若缓存未命中，且通过 supplier 成功创建了非 null 对象，则将其存入缓存
        if (result == null && (result = supplier.get()) != null) {
            SINGLE_OBJECT_POOL.put(key, result);
        }

        // 返回结果：非 null 对象直接返回，null 则返回 null（避免类型转换异常）
        return result != null ? (T) result : null;
    }

    /**
     * 对象放入容器
     * @param value 值
     */
    public static void put(Object value) {
        put(value.getClass().getName(), value);
    }

    public static void put(String key, Object value) {
        SINGLE_OBJECT_POOL.put(key, value);
    }
}
