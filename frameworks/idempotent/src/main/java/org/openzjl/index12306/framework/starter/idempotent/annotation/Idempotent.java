/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.annotation;

import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;

import java.lang.annotation.*;

/**
 * 幂等注解
 *
 * @author zhangjlk
 * @date 2025/10/5 19:08
 */
@Target({ElementType.TYPE, ElementType.METHOD}) // 可以用在类、方法上
@Retention(RetentionPolicy.RUNTIME) // 注解会保留在运行时
@Documented
public @interface Idempotent {

    /**
     * 幂等key
     * 只有在 {@link Idempotent#type()} 为 {@link IdempotentTypeEnum#SPEL} 时生效
     */
    String key() default "";

    /**
     * 触发幂等失败逻辑的时候，返回的错误提示信息
     */
    String message() default "您操作的太快了，请稍后重试";

    /**
     * 验证幂等类型，支持多种幂等方式
     * RestAPi 建议使用 {@link IdempotentTypeEnum#TOKEN} 或者 {@link IdempotentTypeEnum#PARAM}
     * 其他类型幂等验证使用 SPEL 表达式进行验证就行
     */
    IdempotentTypeEnum type() default IdempotentTypeEnum.PARAM;

    /**
     * 验证幂等场景，目前是支持RESTAPI以及消息队列消费的幂等
     */
    IdempotentSceneEnum scene() default IdempotentSceneEnum.RESTAPI;

    /**
     * 防重复令牌 Key 前缀
     * {@link IdempotentSceneEnum#MQ} and {@link IdempotentTypeEnum#SPEL} 时生效
     */
    String uniqueKeyPrefix() default "";

    /**
     * 设置防重令牌 Key 过期时间，单位秒，默认一个小时
     * {@link IdempotentSceneEnum#MQ} and {@link IdempotentTypeEnum#SPEL} 时生效
     */
    long keyTimeout() default 3600L;
}
