/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;

/**
 * 【抽象幂等执行处理器】—— 定义幂等处理的标准流程
 *
 * 设计思想：
 * - 使用“模板方法模式”，把“构建参数 → 执行处理”封装成固定流程
 * - 子类只需实现 buildWrapper()，即可支持不同幂等方式（PARAM / TOKEN / SPEL）
 * - 遵循“开闭原则”：对扩展开放，对修改关闭
 *
 * @author zhangjlk
 * @date 2025/10/6 12:19
 */
public abstract class AbstractIdempotentExecuteHandler implements IdempotentExecuteHandler {

    /**
     * 抽象方法：构建幂等处理所需的参数包装器
     *
     * 说明：
     * - 子类必须实现此方法，根据不同的幂等类型（PARAM / TOKEN / SPEL）生成对应的 IdempotentParamWrapper
     * - 例如：
     *   - PARAM 类型：从方法参数拼接 Key
     *   - TOKEN 类型：从请求头或参数中提取 Token
     *   - SpEL 类型：用 SpEL 表达式解析生成 Key
     *
     * @param joinPoint AOP 方法连接点对象
     * @return 幂等参数包装器（包含注解、连接点、锁 Key 等信息）
     */
    protected abstract IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint);

    /**
     * 模板方法：执行幂等处理逻辑
     *
     * 流程：
     * 1. 调用子类实现的 buildWrapper() 构建参数包装器
     * 2. 设置幂等注解到包装器中
     * 3. 调用 handler() 方法执行具体幂等逻辑（如 Redis 校验、加锁、执行原方法等）
     *
     * @param proceedingJoinPoint AOP 方法连接点
     * @param idempotent          幂等注解对象
     */
    @Override
    public void execute(ProceedingJoinPoint proceedingJoinPoint, Idempotent idempotent) {
        // 1. 调用子类实现的方法，构建参数包装器
        IdempotentParamWrapper idempotentParamWrapper = buildWrapper(proceedingJoinPoint);

        // 2. 设置幂等注解（便于后续使用注解属性，如 key 表达式、TTL 等）
        idempotentParamWrapper.setIdempotent(idempotent);

        // 3. 调用抽象方法 handler()，由子类实现具体的幂等逻辑
        handler(idempotentParamWrapper);
    }
}
