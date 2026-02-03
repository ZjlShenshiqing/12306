/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;

/**
 * 【幂等参数包装类】—— 封装 AOP 切面中处理幂等所需的核心上下文信息
 *
 * 作用：
 * - 在 AOP 切面方法中，将“幂等注解”、“连接点”、“锁 Key”等信息打包成一个对象
 * - 便于后续调用幂等处理器、分布式锁、日志记录等组件时统一传参
 *
 * 其他信息（如 traceId、方法名、参数值等）可以通过这三个字段动态获取或按需扩展，而不是硬编码在包装类里
 *
 * @author zhangjlk
 * @date 2025/10/6 11:42
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public final class IdempotentParamWrapper {

    /**
     * 幂等注解
     */
    private Idempotent idempotent;

    /**
     * AOP 处理连接点
     */
    private ProceedingJoinPoint proceedingJoinPoint;

    /**
     * 锁标识
     */
    private String lockKey;
}
