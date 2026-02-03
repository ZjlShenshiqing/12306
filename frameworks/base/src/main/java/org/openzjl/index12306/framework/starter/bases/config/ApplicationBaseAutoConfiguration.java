/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.bases.config;

import org.openzjl.index12306.framework.starter.bases.ApplicationContextHolder;
import org.openzjl.index12306.framework.starter.bases.init.ApplicationContextPostProcessor;
import org.openzjl.index12306.framework.starter.bases.safa.FastJsonSafeMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 应用基础自动装配
 * 将创建的自定义bean注册到Spring容器中
 * @author zhangjlk
 * @date 2025/9/6 21:56
 */
public class ApplicationBaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean // @ConditionalOnMissingBean：意思是：“只有当项目里还没有这个类型的 Bean 时，才创建！
    public ApplicationContextHolder congoApplicationContextHolder() {
        return new ApplicationContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApplicationContextPostProcessor congoApplicationContextPostProcessor(ApplicationContext applicationContext) {
        return new ApplicationContextPostProcessor(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public FastJsonSafeMode congoFastJsonSafeMode() {
        return new FastJsonSafeMode();
    }
}
