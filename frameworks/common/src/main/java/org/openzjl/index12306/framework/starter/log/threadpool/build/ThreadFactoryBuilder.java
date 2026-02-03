/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.log.threadpool.build;

import org.openzjl.index12306.framework.starter.designpattern.builder.Builder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程工厂构建器，构建者模式
 *
 * @author zhangjlk
 * @date 2025/9/18 10:36
 */
public final class ThreadFactoryBuilder implements Builder<ThreadFactory> {

    private static final long serialVersionUID = 1L;

    /**
     * 底层使用的原始线程工厂（可选）
     * 如果提供了，则在此基础上包装增强功能（如命名、设优先级等）
     */
    private ThreadFactory backingThreadFactory;

    /**
     * 线程名称前缀（必填推荐）
     * 所有由该工厂创建的线程将具有此前缀 + 自增编号，例如：
     *   - "biz-thread-1"
     *   - "biz-thread-2"
     *
     * 目的：便于日志排查、性能监控和线上问题定位
     */
    private String namePrefix;

    /**
     * 是否设置为守护线程（daemon thread）
     *
     * true：守护线程 → 当所有非守护线程结束时，JVM 可以直接退出，不管它是否还在运行
     * false：用户线程 → JVM 必须等待它执行完或中断后才能退出
     *
     * 示例：
     *   - 日志刷盘线程 → 可设为 true（主业务结束就可以停）
     *   - 订单处理线程 → 应设为 false（不能中途丢弃任务）
     */
    private Boolean daemon;

    /**
     * 线程优先级（范围：1~10）
     *
     * Thread.MIN_PRIORITY = 1（最低）
     * Thread.NORM_PRIORITY = 5（默认）
     * Thread.MAX_PRIORITY = 10（最高）
     *
     * 注意：Java 的优先级是提示性的，实际调度依赖操作系统
     */
    private Integer priority;

    /**
     * 未捕获异常处理器
     *
     * 当线程在执行过程中抛出未被捕获的异常时（比如 run() 方法里出现空指针），
     * JVM 会调用这个处理器来处理异常，避免静默失败。
     *
     * 常见用途：
     *   - 打印错误日志
     *   - 上报监控系统
     *   - 触发告警
     */
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    /**
     * 设置一个底层线程工厂（可选）
     * 如果设置了，将在其基础上进行功能增强（如添加命名、优先级控制等）
     *
     * @param backingThreadFactory 原始线程工厂
     * @return 当前构建器实例，支持链式调用
     */
    public ThreadFactoryBuilder threadFactory(ThreadFactory backingThreadFactory) {
        this.backingThreadFactory = backingThreadFactory;
        return this;
    }

    /**
     * 设置线程名称前缀
     *
     * @param namePrefix 如 "worker-", "db-pool-" 等，生成的线程名为：namePrefix + 编号
     * @return 当前构建器实例
     */
    public ThreadFactoryBuilder prefix(String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    /**
     * 设置是否为守护线程
     *
     * @param daemon true 表示创建的线程为守护线程；false 表示普通用户线程
     * @return 当前构建器实例
     */
    public ThreadFactoryBuilder daemon(Boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    /**
     * 设置线程优先级
     *
     * 校验输入是否在合法范围内 [1, 10]
     *
     * @param priority 线程优先级（1~10）
     * @return 当前构建器实例
     * @throws IllegalArgumentException 如果优先级不在有效范围
     */
    public ThreadFactoryBuilder priority(int priority) {
        if (priority < Thread.MIN_PRIORITY) {
            throw new IllegalArgumentException(
                    String.format("Thread priority (%d) must be >= %d", priority, Thread.MIN_PRIORITY));
        }
        if (priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    String.format("Thread priority (%d) must be <= %d", priority, Thread.MAX_PRIORITY));
        }
        this.priority = priority;
        return this;
    }

    /**
     * 设置未捕获异常处理器
     *
     * @param uncaughtExceptionHandler 处理器逻辑，接收两个参数：
     *                                 - t: 出现异常的线程
     *                                 - e: 抛出的异常
     * @return 当前构建器实例
     */
    public ThreadFactoryBuilder uncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
        return this;
    }

    /**
     * 静态工厂方法：创建一个新的 ThreadFactoryBuilder 实例
     * 支持链式调用，是使用构建器的标准入口
     *
     * @return 新的构建器实例
     */
    public static ThreadFactoryBuilder builder() {
        return new ThreadFactoryBuilder();
    }

    /**
     * 构建并返回一个自定义的 ThreadFactory
     *
     * 创建的线程将具备以下特性：
     *   - 名称格式：namePrefix + 编号（如 worker-1）
     *   - 是否为守护线程（daemon）
     *   - 指定优先级（priority）
     *   - 绑定未捕获异常处理器
     *
     * @return 配置好的 ThreadFactory 对象
     */
    @Override
    public ThreadFactory build() {
        return build(this);
    }

    /**
     * 根据 ThreadFactoryBuilder 的配置，构建一个高度可定制的线程工厂（ThreadFactory）
     *
     * 该工厂创建的每一个线程都会具备以下特性：
     *   - 有统一命名格式（如 worker_0, worker_1），便于日志排查和监控
     *   - 可设置是否为守护线程（daemon），控制 JVM 是否等待其结束
     *   - 支持自定义优先级（priority），影响线程调度权重
     *   - 绑定未捕获异常处理器（uncaughtExceptionHandler），防止任务静默失败
     *
     * 此方法采用装饰器模式：基于已有线程工厂（backingThreadFactory）进行功能增强。
     *
     * @param builder 包含所有线程配置信息的构建器对象
     * @return 一个定制化的 ThreadFactory 实例
     */
    private static ThreadFactory build(ThreadFactoryBuilder builder) {
        // 如果用户提供了底层线程工厂，则使用它；否则使用 JDK 默认的工厂作为基础
        final ThreadFactory backingThreadFactory = (null != builder.backingThreadFactory)
                ? builder.backingThreadFactory
                : Executors.defaultThreadFactory();

        // 提取用户配置的各项参数，保存为 final 变量
        final String namePrefix = builder.namePrefix;
        final Boolean daemon = builder.daemon;
        final Integer priority = builder.priority;
        final Thread.UncaughtExceptionHandler handler = builder.uncaughtExceptionHandler;

        // 如果设置了名称前缀，则创建一个原子计数器，用于生成唯一递增编号
        // 否则设为 null，表示不需要编号
        final AtomicLong count = (null == namePrefix) ? null : new AtomicLong();

        // 返回一个新的 ThreadFactory 实现
        // 它的 newThread(runnable) 方法会按照上述规则创建并配置线程
        return r -> {
            // 第一步：由底层工厂创建原始线程（保证类加载器、上下文等一致）
            final Thread thread = backingThreadFactory.newThread(r);

            // 第二步：如果配置了线程名称前缀，则设置带编号的名字，例如：worker_0, worker_1
            if (null != namePrefix) {
                thread.setName(namePrefix + "_" + count.getAndIncrement());
            }

            // 第三步：如果指定了是否为守护线程，则设置该属性
            // true：JVM 可以不等待此线程直接退出（适合后台任务）
            // false：JVM 必须等待此线程执行完才能退出（适合业务任务）
            if (null != daemon) {
                thread.setDaemon(daemon);
            }

            // 第四步：如果设置了优先级，则应用到线程上
            // 注意：Java 优先级是提示性建议，实际调度依赖操作系统
            if (null != priority) {
                thread.setPriority(priority);
            }

            // 第五步：如果设置了未捕获异常处理器，则绑定到该线程
            // 当线程 run() 方法中抛出未被捕获的异常时，会触发此处理器
            // 常见用途：打印错误日志、上报监控系统、发送告警
            if (null != handler) {
                thread.setUncaughtExceptionHandler(handler);
            }

            // 返回线程对象
            return thread;
         };
    }
}
