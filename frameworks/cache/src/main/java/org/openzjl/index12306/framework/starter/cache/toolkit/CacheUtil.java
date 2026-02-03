/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.cache.toolkit;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 缓存工具类
 *
 * <p>
 * 提供缓存操作中常用的工具方法，包括缓存 key 构建、缓存值校验等功能。
 * 统一缓存 key 的命名规范，简化缓存值的空值判断逻辑。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *     <li>构建符合规范的缓存 key（多层级拼接）。</li>
 *     <li>判断缓存值是否为空或空白。</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:03
 */
public final class CacheUtil {

    /**
     * 缓存 key 拼接分隔符。
     * <p>
     * 使用点号（.）作为多层级 key 的分隔符，便于形成层次化的缓存命名空间。
     * 例如：user.profile.123 表示用户资料相关的缓存。
     * </p>
     */
    private static final String SPLICING_OPERATOR = ".";

    /**
     * 构建缓存 key 标识。
     * <p>
     * 将多个 key 片段使用分隔符拼接成完整的缓存 key，并对每个片段进行非空校验。
     * 适用于构建层次化的缓存命名空间，如 "user.profile.123"。
     * </p>
     *
     * <p>示例：</p>
     * <pre>
     * String key = CacheUtil.buildKey("user", "profile", "123");
     * // 结果：user.profile.123
     * </pre>
     *
     * @param keys 缓存 key 片段（可变参数，每个片段不能为空或空字符串）
     * @return 拼接后的完整缓存 key
     * @throws RuntimeException 当任意 key 片段为 null 或空字符串时抛出
     */
    public static String buildKey(String... keys) {
        Stream.of(keys).forEach(each -> Optional.ofNullable(Strings.emptyToNull(each)).orElseThrow(() -> new RuntimeException("构建缓存 key 不能为空")));
        return Joiner.on(SPLICING_OPERATOR).join(keys);
    }

    /**
     * 判断缓存值是否为空或空白。
     * <p>
     * 统一处理缓存值的空值判断逻辑，支持以下情况：
     * </p>
     * <ul>
     *     <li>对象为 {@code null}。</li>
     *     <li>字符串类型且为空字符串或仅包含空白字符。</li>
     * </ul>
     *
     * @param cacheVal 缓存值对象
     * @return {@code true} 表示缓存值为空或空白，{@code false} 表示缓存值有效
     */
    public static boolean isNullOrBlank(Object cacheVal) {
        return cacheVal == null || (cacheVal instanceof String && Strings.isNullOrEmpty((String) cacheVal));
    }
}
