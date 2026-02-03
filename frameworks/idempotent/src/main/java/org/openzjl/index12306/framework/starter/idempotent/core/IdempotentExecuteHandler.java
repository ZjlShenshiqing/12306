/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;

/**
 * 幂等执行处理器
 *
 * @author zhangjlk
 * @date 2025/10/6 11:41
 */
public interface IdempotentExecuteHandler {

    /**
     * 幂等处理逻辑
     *
     * @param wrapper 幂等参数包装器
     */
    void handler(IdempotentParamWrapper wrapper);

    /**
     * 执行幂等处理逻辑
     *
     * @param proceedingJoinPoint   AOP 方法处理
     * @param idempotent            幂等注解
     */
    void execute(ProceedingJoinPoint proceedingJoinPoint, Idempotent idempotent);

    /**
     * 异常流程处理
     */
    default void exceptionProcessing() {

    }

    /**
     * 后置处理
     */
    default void postProcessing() {

    }
}
