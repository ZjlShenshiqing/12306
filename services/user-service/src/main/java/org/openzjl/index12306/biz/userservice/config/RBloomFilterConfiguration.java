/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置类
 * <p>
 * 负责创建和配置用户注册相关的布隆过滤器 Bean 实例
 * 使用 Redisson 客户端操作 Redis 实现的布隆过滤器
 * </p>
 *
 * @author zhangjlk
 * @date 2026/2/2 下午8:28
 */
@Configuration
@EnableConfigurationProperties(UserRegisterBloomFilterProperties.class)
public class RBloomFilterConfiguration {

    /**
     * 创建用户注册缓存穿透布隆过滤器 Bean
     * <p>
     * 该布隆过滤器用于防止用户注册过程中的缓存穿透攻击
     * 存储已注册的用户名或手机号等唯一标识
     * </p>
     *
     * @param redissonClient                  Redisson 客户端实例，用于操作 Redis
     * @param userRegisterBloomFilterProperties 布隆过滤器配置属性，包含名称、容量和误判率等参数
     * @return 配置完成的 RBloomFilter 实例，泛型为 String 类型，用于存储用户唯一标识
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient, UserRegisterBloomFilterProperties userRegisterBloomFilterProperties) {
        // 从 Redisson 客户端获取指定名称的布隆过滤器实例
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter(userRegisterBloomFilterProperties.getName());
        
        // 尝试初始化布隆过滤器，设置预期插入量和误判概率
        // 若布隆过滤器已存在则不会重复初始化
        cachePenetrationBloomFilter.tryInit(userRegisterBloomFilterProperties.getExpectedInsertions(), userRegisterBloomFilterProperties.getFalseProbability());
        
        // 返回配置好的布隆过滤器实例
        return cachePenetrationBloomFilter;
    }
}
