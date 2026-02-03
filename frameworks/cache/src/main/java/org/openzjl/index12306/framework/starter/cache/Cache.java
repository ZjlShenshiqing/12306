/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.cache;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;

/**
 * 缓存操作统一接口。
 * <p>
 * 提供常见的缓存操作抽象，便于在不同缓存实现（如 Redis、Caffeine、Hazelcast 等）之间切换，
 * 同时保持业务代码的一致性。
 * </p>
 *
 * <p>说明：</p>
 * <ul>
 *     <li>所有 key 参数使用 {@code @NotBlank} 校验，确保非空且去除前后空格。</li>
 *     <li>支持泛型返回值，避免手动类型转换。</li>
 *     <li>批量操作提供更好的性能（减少网络往返）。</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:00
 */
public interface Cache {

    /**
     * 根据 key 获取缓存值并转换为指定类型。
     * <p>
     * 注：{@code @NotBlank} = {@code @NotNull} + {@code @NotEmpty} + {@code trim()}
     * </p>
     *
     * @param key   缓存键（非空且去除前后空格）
     * @param clazz 目标类型
     * @param <T>   泛型类型
     * @return 缓存值，若不存在则返回 {@code null}
     */
    <T> T get(@NotBlank String key, Class<T> clazz);

    /**
     * 放入缓存键值对。
     * <p>
     * 若 key 已存在，则覆盖原值。
     * </p>
     *
     * @param key   缓存键（非空且去除前后空格）
     * @param value 缓存值
     */
    void put(@NotBlank String key, Object value);

    /**
     * 如果 keys 全部都不存在，则新增，返回true，反之false
     */
    Boolean putIfAllAbsent(@NotNull Collection<String> keys);

    /**
     * 删除指定 key 的缓存。
     *
     * @param key 缓存键（非空且去除前后空格）
     * @return {@code true} 表示删除成功，{@code false} 表示 key 不存在或删除失败
     */
    Boolean delete(@NotBlank String key);

    /**
     * 批量删除指定 keys 的缓存。
     *
     * @param keys 缓存键集合（非空）
     * @return 实际删除数量
     */
    Long delete(@NotNull Collection<String> keys);

    /**
     * 检查指定 key 是否存在于缓存中。
     *
     * @param key 缓存键（非空且去除前后空格）
     * @return {@code true} 表示存在，{@code false} 表示不存在
     */
    Boolean hasKey(@NotBlank String key);

    /**
     * 获取底层缓存实现的原生实例。
     * <p>
     * 用于执行接口未覆盖的特定操作，如 Redis 的 Lua 脚本、管道操作等。
     * 使用时需根据具体实现进行类型转换。
     * </p>
     *
     * @return 底层缓存实例（如 {@code RedisTemplate}、{@code com.github.benmanes.caffeine.cache.Cache} 等）
     */
    Object getInstance();
}
