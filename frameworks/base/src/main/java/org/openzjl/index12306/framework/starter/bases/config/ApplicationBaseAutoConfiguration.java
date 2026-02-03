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
 * 应用基础自动装配配置类
 * <p>
 * 负责将框架基础组件自动注册到 Spring 容器中，
 * 确保应用启动时能够使用这些基础服务。
 * </p>
 * 
 * <p><strong>配置内容：</strong></p>
 * <ul>
 *   <li>ApplicationContextHolder：Spring 容器持有器，用于在非 Spring 管理的类中获取 Bean</li>
 *   <li>ApplicationContextPostProcessor：应用上下文后置处理器，用于在应用启动时执行初始化逻辑</li>
 *   <li>FastJsonSafeMode：FastJson 安全模式配置，防止反序列化漏洞</li>
 * </ul>
 * 
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>作为框架的自动配置类，在应用启动时自动生效</li>
 *   <li>通过条件注解确保 Bean 只在需要时创建，避免重复配置</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2025/9/6 21:56
 */
public class ApplicationBaseAutoConfiguration {

    /**
     * 创建 ApplicationContextHolder Bean
     * <p>
     * ApplicationContextHolder 是一个 Spring 容器持有器，实现了 ApplicationContextAware 接口，
     * 用于在非 Spring 管理的类中通过静态方法获取 Spring 容器中的 Bean。
     * </p>
     * 
     * <p><strong>条件：</strong></p>
     * <ul>
     *   <li>当容器中不存在 ApplicationContextHolder 类型的 Bean 时才创建</li>
     *   <li>使用 @ConditionalOnMissingBean 注解确保 Bean 唯一性</li>
     * </ul>
     * 
     * <p><strong>使用方式：</strong></p>
     * <pre>
     * // 在任何类中获取 Spring Bean
     * UserService userService = ApplicationContextHolder.getBean(UserService.class);
     * </pre>
     *
     * @return ApplicationContextHolder 实例，用于获取 Spring 容器中的 Bean
     */
    @Bean
    @ConditionalOnMissingBean(ApplicationContextHolder.class) // 当容器中不存在 ApplicationContextHolder 类型的 Bean 时才创建
    public ApplicationContextHolder congoApplicationContextHolder() {
        // 实例化 ApplicationContextHolder，Spring 会自动调用其 setApplicationContext 方法注入应用上下文
        return new ApplicationContextHolder();
    }

    /**
     * 创建 ApplicationContextPostProcessor Bean
     * <p>
     * ApplicationContextPostProcessor 是一个应用上下文后置处理器，
     * 用于在应用启动时执行一些初始化逻辑，如注册监听器、初始化配置等。
     * </p>
     * 
     * <p><strong>条件：</strong></p>
     * <ul>
     *   <li>当容器中不存在 ApplicationContextPostProcessor 类型的 Bean 时才创建</li>
     * </ul>
     * 
     * <p><strong>参数：</strong></p>
     * <ul>
     *   <li>applicationContext：Spring 应用上下文，由 Spring 自动注入</li>
     * </ul>
     *
     * @param applicationContext Spring 应用上下文，包含所有 Bean 定义和环境配置
     * @return ApplicationContextPostProcessor 实例，用于在应用启动时处理应用上下文
     */
    @Bean
    @ConditionalOnMissingBean(ApplicationContextPostProcessor.class) // 当容器中不存在 ApplicationContextPostProcessor 类型的 Bean 时才创建
    public ApplicationContextPostProcessor congoApplicationContextPostProcessor(ApplicationContext applicationContext) {
        // 实例化 ApplicationContextPostProcessor，并传入应用上下文
        return new ApplicationContextPostProcessor(applicationContext);
    }

    /**
     * 创建 FastJsonSafeMode Bean
     * <p>
     * FastJsonSafeMode 用于启用 FastJson 的安全模式，
     * 防止反序列化漏洞，提高应用安全性。
     * </p>
     * 
     * <p><strong>条件：</strong></p>
     * <ul>
     *   <li>当容器中不存在 FastJsonSafeMode 类型的 Bean 时才创建</li>
     * </ul>
     * 
     * <p><strong>作用：</strong></p>
     * <ul>
     *   <li>启用 FastJson 的 safeMode，限制反序列化的类型</li>
     *   <li>防止恶意构造的 JSON 字符串导致的安全漏洞</li>
     * </ul>
     *
     * @return FastJsonSafeMode 实例，用于启用 FastJson 的安全模式
     */
    @Bean
    @ConditionalOnMissingBean(FastJsonSafeMode.class) // 当容器中不存在 FastJsonSafeMode 类型的 Bean 时才创建
    public FastJsonSafeMode congoFastJsonSafeMode() {
        // 实例化 FastJsonSafeMode，启用 FastJson 安全模式
        return new FastJsonSafeMode();
    }
}
