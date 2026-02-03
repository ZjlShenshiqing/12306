/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户注册布隆过滤器属性配置类
 * <p>
 * 用于配置用户注册过程中防止缓存穿透的布隆过滤器参数
 * 通过 Spring Boot 的 @ConfigurationProperties 注解绑定配置文件中的属性
 * </p>
 *
 * @author zhangjlk
 * @date 2026/2/2 下午8:32
 */
@Data
@ConfigurationProperties(prefix = UserRegisterBloomFilterProperties.PREFIX)
public final class UserRegisterBloomFilterProperties {

    /**
     * 配置属性前缀
     * <p>用于在 application.yml 或 application.properties 中配置相关属性</p>
     */
    public static final String PREFIX = "framework.cache.redis.bloom-filter.user-register";

    /**
     * 用户注册布隆过滤器实例名称
     * <p>用于在 Redis 中唯一标识该布隆过滤器实例</p>
     */
    private String name = "user_register_cache_penetration_bloom_filter";

    /**
     * 每个元素的预期插入量
     * <p>布隆过滤器的容量配置，应根据系统预期的用户注册量设置</p>
     * <p>默认值 64L，实际使用中应根据业务规模调整</p>
     */
    private Long expectedInsertions = 64L;

    /**
     * 预期错误概率
     * <p>布隆过滤器的误判率，值越小，所需空间越大</p>
     * <p>默认值 0.03D（3%），平衡了空间开销和误判率</p>
     */
    private Double falseProbability = 0.03D;
}
