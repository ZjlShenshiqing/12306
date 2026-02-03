/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.bases;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 解决 在非 Spring 管理的对象中无法使用 @Autowired 注入 Spring Bean 的问题。通过实现 ApplicationContextAware 接口，
 * 把 Spring 容器（ApplicationContext）保存到一个全局可用的工具类中，从而可以在任何地方手动获取所需的 Spring Bean。
 *
 * 可以手动从容器中获取任何 Spring Bean
 * @author zhangjlk
 * @date 2025/9/6 21:49
 */
// 实现接口：implements ApplicationContextAware → Spring 启动时会自动把“容器”传给它。
public class ApplicationContextHolder implements ApplicationContextAware {

    /**
     * 把 Spring 容器存到一个静态变量里，谁都能用
     */
    private static ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT = applicationContext;
    }

    /**
     * 根据类型获取bean
     * @param clazz 类型
     * @return bean
     *
     * 具体使用方式如下所示：
     * UserService userService = ApplicationContextHolder.getBean(UserService.class);
     */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * 根据名称获取bean
     * @param name 名称
     *
     * 具体使用方式如下所示：
     * @Service("emailSender")
     * public class EmailSender
     *
     * String senderType = "emailSender"; // 可能从数据库读取
     * // 根据名字动态获取 Bean
     * Sender sender = (Sender) ApplicationContextHolder.getBean(senderType);
     */
    public static Object getBean(String name) {
        return CONTEXT.getBean(name);
    }

    /**
     * 根据名字 + 类型拿 Bean
     * 使用示例如下所示：
     * EmailSender sender = ApplicationContextHolder.getBean("emailSender", EmailSender.class);
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * 所有实现了某个接口的 Spring 对象（Bean），并组装成Map
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * 看看类上是否有注解
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
         return CONTEXT.findAnnotationOnBean(beanName, annotationType);
    }

    /**
     * 获取整个Spring容器对象
     * @return 容器对象
     */
    public static ApplicationContext getInstance() {
        return CONTEXT;
    }
}
