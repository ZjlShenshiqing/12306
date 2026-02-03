/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.log.threadpool.build;

import org.openzjl.index12306.framework.starter.log.toolkit.Assert;
import org.openzjl.index12306.framework.starter.designpattern.builder.Builder;

import java.math.BigDecimal;
import java.util.concurrent.*;

/**
 * 线程池构建器（Builder 模式）
 * 用于制造线程
 *
 * 用于以链式调用的方式便捷地创建自定义线程池。
 * 提供了合理的默认值，同时也支持高度可配置化。
 *
 * 示例：
 *     ThreadPoolExecutor executor = ThreadPoolBuilder.builder()
 *         .corePoolSize(10)
 *         .maximumPoolSize(20)
 *         .workQueue(new ArrayBlockingQueue<>(100))
 *         .threadFactory("my-pool-", true)  // 前缀 + 守护线程
 *         .build();
 *
 * @author zhangjlk
 * @date 2025/9/18 10:36
 */
public final class ThreadPoolBuilder implements Builder<ThreadPoolExecutor> {

    // 默认核心线程数：根据 CPU 核心数 ×5 计算得出（适用于 I/O 密集型任务）
    private int corePoolSize = calculateCoreNum();

    // 最大线程数 = 核心线程数 + 核心线程数的一半（即 1.5 倍）
    // 例如：core=20 → max=30
    private int maximumPoolSize = corePoolSize + (corePoolSize >>> 1);

    // 非核心线程空闲存活时间，默认为 1 微秒
    private long keepAliveTime = 1;

    // 时间单位，默认使用 MICROSECONDS（微秒），非常短，适合快速回收非核心线程
    private TimeUnit timeUnit = TimeUnit.MICROSECONDS;

    // 工作队列：默认使用有界队列 LinkedBlockingQueue，容量为 4096
    // 防止无限制堆积导致内存溢出（OOM）
    private BlockingQueue workQueue = new LinkedBlockingQueue(4096);

    // 拒绝策略：默认为 AbortPolicy → 超负荷时抛出 RejectedExecutionException
    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    // 是否设置线程为守护线程（daemon）
    // false 表示主线程结束前，这些工作线程仍会继续执行完任务
    private Boolean isDaemon = false;

    // 线程名称前缀，便于日志排查和监控（如 "biz-thread-"）
    private String threadNamePrefix;

    // 自定义线程工厂（优先级高于 threadNamePrefix + isDaemon 的组合）
    private ThreadFactory threadFactory;

    /**
     * 根据 CPU 核心数动态计算线程池核心线程数量
     *
     * @return 核心线程数量
     */
    private Integer calculateCoreNum() {
        // 返回当前机器 可用的 CPU 核心数量（包括超线程）
        int cpuCoreNum = Runtime.getRuntime().availableProcessors();
        // 一个核心处理5个线程
        return new BigDecimal(cpuCoreNum).divide(new BigDecimal("0.2")).intValue();
    }

    /**
     * 设置自定义线程工厂（最高优先级）
     * 如果设置了此工厂，则忽略 threadNamePrefix 和 isDaemon 的配置
     *
     * @param threadFactory 自定义线程工厂
     * @return 当前构建器实例，支持链式调用
     */
    public ThreadPoolBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    /**
     * 设置核心线程数
     *
     * @param corePoolSize 核心线程数量
     * @return 当前构建器实例
     */
    public ThreadPoolBuilder corePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    /**
     * 设置最大线程数
     * 若设置的最大值小于当前核心线程数，则同步调整 corePoolSize，确保 max >= core
     *
     * @param maximumPoolSize 最大线程数量
     * @return 当前构建器实例
     */
    public ThreadPoolBuilder maximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        if (maximumPoolSize < this.corePoolSize) {
            this.corePoolSize = maximumPoolSize;
        }
        return this;
    }

    /**
     * 设置线程名称前缀和是否为守护线程
     * 内部会自动创建一个默认的 ThreadFactory 来实现命名和 daemon 控制
     *
     * @param threadNamePrefix 线程名前缀，如 "order-pool-"
     * @param isDaemon         是否为守护线程（true: JVM 退出时不等待；false: 会等待任务完成）
     * @return 当前构建器实例
     */
    public ThreadPoolBuilder threadFactory(String threadNamePrefix, Boolean isDaemon) {
        this.threadNamePrefix = threadNamePrefix;
        this.isDaemon = isDaemon;
        return this;
    }

    /**
     * 设置非核心线程空闲存活时间（使用默认时间单位：微秒）
     *
     * @param keepAliveTime 存活时间数值
     * @return 当前构建器实例
     */
    public ThreadPoolBuilder keepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    /**
     * 设置非核心线程空闲存活时间和单位
     * 更灵活的配置方式
     *
     * @param keepAliveTime 存活时间数值
     * @param timeUnit      时间单位（如 SECONDS、MILLISECONDS）
     * @return 当前构建器实例
     */
    public ThreadPoolBuilder keepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        return this;
    }

    /**
     * 设置拒绝策略
     * 常见策略：
     *   - AbortPolicy: 抛异常（默认）
     *   - CallerRunsPolicy: 由提交任务的线程自己执行
     *   - DiscardPolicy: 静默丢弃
     *   - DiscardOldestPolicy: 丢弃队列中最老的任务
     *
     * @param rejectedExecutionHandler 拒绝处理器
     * @return 当前构建器实例
     */
    public ThreadPoolBuilder rejected(RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        return this;
    }

    /**
     * 设置任务队列
     * 推荐使用有界队列防止资源耗尽
     *
     * @param workQueue 自定义阻塞队列（如 ArrayBlockingQueue、LinkedBlockingQueue）
     * @return 当前构建器实例
     */
    public ThreadPoolBuilder workQueue(BlockingQueue workQueue) {
        this.workQueue = workQueue;
        return this;
    }

    /**
     * 静态工厂方法：获取一个新的 ThreadPoolBuilder 实例
     * 支持链式调用，是 Builder 模式的标准入口
     *
     * @return 新的构建器实例
     */
    public static ThreadPoolBuilder builder() {
        return new ThreadPoolBuilder();
    }

    /**
     * 构建并返回一个配置完成的 ThreadPoolExecutor 实例
     *
     * 该方法是 Builder 模式的最终产出步骤：
     * 根据之前设置的各项参数（核心线程数、队列、拒绝策略等），
     * 创建一个生产级可用的线程池。
     *
     * 如果用户未提供自定义 ThreadFactory，则根据 threadNamePrefix 和 isDaemon 自动生成一个。
     *
     * @return 配置好的 ThreadPoolExecutor 对象
     * @throws IllegalStateException 如果线程池创建失败（如参数非法）
     */
    @Override
    public ThreadPoolExecutor build() {
        if (threadFactory == null) {
            Assert.notEmpty(threadNamePrefix, "threadNamePrefix must not be empty");
            threadFactory = ThreadFactoryBuilder.builder().prefix(threadNamePrefix).daemon(isDaemon).build();

        }

        // 线程池管理工具
        ThreadPoolExecutor executorService;
        try {
            executorService = new ThreadPoolExecutor(
                    corePoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    timeUnit,
                    workQueue,
                    threadFactory,
                    rejectedExecutionHandler
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex);
        }
        return executorService;
    }
}
