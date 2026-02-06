/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置类
 * <p>
 * 负责配置 RocketMQ 相关的 Bean，确保 RocketMQTemplate 能够被正确创建和注入
 * </p>
 *
 * @author zhangjlk
 * @date 2026/2/3 上午10:48
 */
@Configuration
public class RocketMQConfiguration {
    
    /**
     * 创建 RocketMQTemplate Bean
     * <p>
     * 当没有 RocketMQTemplate Bean 且配置了 rocketmq.name-server 时，创建默认的 RocketMQTemplate
     * </p>
     *
     * @return RocketMQTemplate 实例
     */
    @Bean
    @ConditionalOnMissingBean(RocketMQTemplate.class)
    @ConditionalOnProperty("rocketmq.name-server")
    public RocketMQTemplate rocketMQTemplate() {
        return new RocketMQTemplate();
    }
    
}
