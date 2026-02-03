/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * Redis Key 序列化器（带前缀支持）。
 * <p>
 * 实现 {@link RedisSerializer} 接口，为 Redis key 提供统一的序列化/反序列化逻辑。
 * 主要功能包括自动添加 key 前缀、字符编码转换等，便于区分不同应用或环境的缓存数据。
 * </p>
 *
 * <p>特性：</p>
 * <ul>
 *     <li>序列化时自动为 key 添加配置的前缀。</li>
 *     <li>支持自定义字符编码（如 UTF-8）。</li>
 *     <li>通过 {@link InitializingBean} 在 Spring 容器初始化时完成字符集配置。</li>
 * </ul>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>多环境部署时避免 key 冲突（如 dev:user:123、prod:user:123）。</li>
 *     <li>多应用共享 Redis 实例时的命名空间隔离。</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:01
 */
@RequiredArgsConstructor
public class RedisKeySerializer implements InitializingBean, RedisSerializer<String> {

    /**
     * key 前缀字符串。
     * <p>
     * 在序列化时会自动拼接到原始 key 前面，用于命名空间隔离。
     * </p>
     */
    private final String keyPrefix;

    /**
     * 字符编码名称。
     * <p>
     * 用于指定字符串与字节数组之间转换的编码格式，如 "UTF-8"。
     * </p>
     */
    private final String charsetName;

    /**
     * 字符集对象。
     * <p>
     * 由 {@link #charsetName} 解析得到，在 {@link #afterPropertiesSet()} 中初始化。
     * </p>
     */
    private Charset charset;

    /**
     * 序列化 key 为字节数组。
     * <p>
     * 将原始 key 添加前缀后转换为字节数组，供 Redis 存储使用。
     * </p>
     *
     * @param key 原始 key（不含前缀）
     * @return 序列化后的字节数组
     * @throws SerializationException 序列化异常
     */
    @Override
    public byte[] serialize(String key) throws SerializationException {
        String builderKey = keyPrefix + key;
        return builderKey.getBytes(charset);
    }

    /**
     * 反序列化字节数组为 key 字符串。
     * <p>
     * 将 Redis 中存储的字节数组还原为字符串形式的 key（包含前缀）。
     * </p>
     *
     * @param bytes 字节数组
     * @return 反序列化后的 key 字符串（包含前缀）
     * @throws SerializationException 反序列化异常
     */
    @Override
    public String deserialize(byte[] bytes) throws SerializationException {
        return new String(bytes, charset);
    }

    /**
     * Spring 容器初始化后的回调方法。
     * <p>
     * 根据 {@link #charsetName} 初始化 {@link #charset} 对象，
     * 确保序列化/反序列化时使用正确的字符编码。
     * </p>
     *
     * @throws Exception 字符集解析异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        charset = Charset.forName(charsetName);
    }
}
