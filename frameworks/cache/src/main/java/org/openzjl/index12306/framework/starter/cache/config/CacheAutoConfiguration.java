/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.cache.config;

import lombok.AllArgsConstructor;
import org.openzjl.index12306.framework.starter.cache.RedisKeySerializer;
import org.openzjl.index12306.framework.starter.cache.StringRedisTemplateProxy;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存配置自动装配类
 *
 * <p>
 * 基于 Spring Boot 自动配置机制，统一注册缓存相关的核心 Bean，包括：
 * Redis key 序列化器、布隆过滤器、Redis 模板代理等。
 * 通过配置属性控制各组件的启用与参数设置。
 * </p>
 *
 * <p>主要功能：</p>
 * <ul>
 *     <li>自动配置带前缀的 Redis key 序列化器。</li>
 *     <li>条件性启用布隆过滤器防缓存穿透。</li>
 *     <li>提供增强的 Redis 模板代理，集成超时、前缀等特性。</li>
 * </ul>
 *
 * <p>配置属性：</p>
 * <ul>
 *     <li>{@link RedisDistributedProperties}：Redis 分布式缓存基础配置。</li>
 *     <li>{@link BloomFilterPenetrateProperties}：布隆过滤器防穿透配置。</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:06
 */
@AllArgsConstructor
@EnableConfigurationProperties({RedisDistributedProperties.class, BloomFilterPenetrateProperties.class})
public class CacheAutoConfiguration {

    /**
     * Redis 分布式缓存配置属性。
     */
    private final RedisDistributedProperties redisDistributedProperties;

    /**
     * 创建 Redis Key 序列化器 Bean。
     * <p>
     * 根据配置的前缀和字符编码创建序列化器，用于统一处理 Redis key 的前缀添加和编码转换。
     * </p>
     *
     * @return Redis key 序列化器实例
     */
    @Bean
    public RedisKeySerializer redisKeySerializer() {
        String prefix = redisDistributedProperties.getPrefix();
        String prefixCharset = redisDistributedProperties.getPrefixCharset();
        return new RedisKeySerializer(prefix, prefixCharset);
    }

    /**
     * 创建缓存穿透防护布隆过滤器 Bean。
     * <p>
     * 仅在配置 {@code framework.cache.redis.bloom-filter.default.enabled=true} 时启用。
     * 使用 Redisson 客户端创建布隆过滤器，并根据配置的预期插入数量和误判率进行初始化。
     * </p>
     *
     * @param redissonClient                 Redisson 客户端
     * @param bloomFilterPenetrateProperties 布隆过滤器配置属性
     * @return 初始化完成的布隆过滤器实例
     */
    @Bean
    @ConditionalOnProperty(prefix = BloomFilterPenetrateProperties.PREFIX, name = "enabled", havingValue = "true")
    public RBloomFilter<String> cachePenetrationBloomFilter(RedissonClient redissonClient, BloomFilterPenetrateProperties bloomFilterPenetrateProperties) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter(bloomFilterPenetrateProperties.getName());
        cachePenetrationBloomFilter.tryInit(bloomFilterPenetrateProperties.getExpectedInsertions(), bloomFilterPenetrateProperties.getFalseProbability());
        return cachePenetrationBloomFilter;
    }

    /**
     * 创建 StringRedisTemplate 代理 Bean。
     * <p>
     * 对标准的 {@link StringRedisTemplate} 进行增强包装，集成以下特性：
     * </p>
     * <ul>
     *     <li>自动应用自定义的 key 序列化器（前缀支持）。</li>
     *     <li>集成默认超时配置。</li>
     *     <li>提供 Redisson 客户端支持的高级操作。</li>
     * </ul>
     *
     * @param redisKeySerializer       Redis key 序列化器
     * @param stringRedisTemplate      Spring 标准 Redis 模板
     * @param redissonClient           Redisson 客户端
     * @return 增强的 Redis 模板代理实例
     */
    @Bean
    public StringRedisTemplateProxy stringRedisTemplateProxy(
            RedisKeySerializer redisKeySerializer,
            StringRedisTemplate stringRedisTemplate,
            RedissonClient redissonClient) {
        stringRedisTemplate.setKeySerializer(redisKeySerializer);
        return new StringRedisTemplateProxy(stringRedisTemplate, redisDistributedProperties, redissonClient);
    }
}
