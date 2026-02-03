/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式缓存配置属性。
 * <p>
 * 通过 {@code @ConfigurationProperties} 自动绑定配置文件中以 {@code framework.cache.redis} 为前缀的属性，
 * 用于统一管理 Redis 缓存的行为配置（如 key 前缀、编码、超时等）。
 * </p>
 *
 * <p>配置示例：</p>
 * <pre>
 * framework:
 *   cache:
 *     redis:
 *       prefix: "myapp:"
 *       prefix-charset: "UTF-8"
 *       value-timeout: 3600
 *       value-time-unit: SECONDS
 * </pre>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:05
 */
@Data
@ConfigurationProperties(prefix = RedisDistributedProperties.PREFIX)
public class RedisDistributedProperties {

    /**
     * 配置属性前缀常量。
     */
    public static final String PREFIX = "framework.cache.redis";

    /**
     * 缓存 key 的统一前缀。
     * <p>
     * 用于区分不同应用或环境的缓存数据，避免 key 冲突。
     * 例如设置为 "myapp:" 后，实际存储的 key 为 "myapp:userInfo"。
     * </p>
     */
    private String prefix = "";

    /**
     * key 前缀的字符编码。
     * <p>
     * 默认使用 UTF-8 编码，确保中文等多字节字符的正确处理。
     * </p>
     */
    private String prefixCharset = "UTF-8";

    /**
     * 缓存值的默认超时时间（数值部分）。
     * <p>
     * 与 {@link #valueTimeUnit} 配合使用，构成完整的超时配置。
     * 默认 30000，配合默认时间单位 SECONDS，即 30000 秒。
     * </p>
     */
    private Long valueTimeout = 30000L;

    /**
     * 缓存值的超时时间单位。
     * <p>
     * 与 {@link #valueTimeout} 配合使用。
     * 默认为 {@code TimeUnit.SECONDS}（秒）。
     * </p>
     */
    private TimeUnit valueTimeUnit = TimeUnit.SECONDS;
}
