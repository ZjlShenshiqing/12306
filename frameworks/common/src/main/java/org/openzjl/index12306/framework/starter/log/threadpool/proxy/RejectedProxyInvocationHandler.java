/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.log.threadpool.proxy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.framework.starter.log.threadpool.build.ThreadPoolBuilder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 线程池拒绝策略的代理处理器（用于监控和告警）
 *
 * 当任务被线程池拒绝时，通过 JDK 动态代理拦截调用，
 * 实现两个增强功能：
 *   1. 统计被拒绝的任务总数（原子递增）
 *   2. 记录错误日志，并可在此处触发报警（如短信、邮件、MQ 上报）
 *
 * 它配合 {@link ThreadPoolBuilder} 使用，提升系统的可观测性和稳定性。
 *
 * @author zhangjlk
 * @date 2025/9/18 10:36
 */
@Slf4j
@AllArgsConstructor
public class RejectedProxyInvocationHandler implements InvocationHandler {

    /**
     * 被代理的目标对象（原始的拒绝策略，如 AbortPolicy、CallerRunsPolicy 等）
     * 所有方法最终都会委托给它执行，保证原有逻辑不变
     */
    private final Object target;

    /**
     * 原子计数器，用于记录累计被拒绝的任务数量
     * 多线程环境下安全递增，可用于监控系统采集指标
     * 例如：Prometheus 抓取 rejectCount 变化趋势
     */
    private final AtomicLong rejectCount;

    /**
     * 拦截对代理对象的任意方法调用
     *
     * 当线程池执行拒绝策略时（如调用 rejectedExecution(runnable, executor)），
     * 实际上是调用了代理对象的方法，该方法会被此 invoke() 拦截。
     *
     * @param proxy      生成的代理对象
     * @param method     被调用的方法
     * @param args       方法参数
     * @return           方法执行结果
     * @throws Throwable 若原方法抛出异常，则包装后重新抛出
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 【增强1】计数 +1：表示有一个任务被拒绝了
        rejectCount.incrementAndGet();

        try {
            // 【增强2】打印错误日志，提示“发生了拒绝”
            log.error("线程池执行拒绝策略，此处模拟报警...");

            // 将方法调用转发给原始目标对象（如 AbortPolicy.rejectedExecution(...)）
            // 这一步才是真正执行原有的拒绝逻辑（比如抛出 RejectedExecutionException）
            return method.invoke(target, args);
        }
        catch (InvocationTargetException ex) {
            // 如果目标方法内部抛出了异常
            // 那么 InvocationTargetException 会包装它
            // 我们需要把原始异常“解包”并重新抛出，保证行为一致
            throw ex.getCause();
        }
        // 注意：不需要 catch IllegalAccessException，因为动态代理已确保可访问
    }
}
