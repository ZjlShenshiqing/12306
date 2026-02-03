/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.config;

import cn.hippo4j.common.executor.support.BlockingQueueTypeEnum;
import cn.hippo4j.core.executor.DynamicThreadPool;
import cn.hippo4j.core.executor.support.ThreadPoolBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Hippo4j动态线程池配置
 * <p>
 * Hippo4j简介：
 * - Hippo4j是一个动态线程池框架，可以在运行时动态调整线程池参数
 * - 支持通过Web界面实时监控和调整线程池配置，无需重启应用
 * - 提供了丰富的监控指标：线程数、队列大小、任务执行时间等
 * <p>
 * 动态线程池的优势：
 * 1. 实时调整：根据业务负载动态调整线程池参数，无需重启应用
 * 2. 监控告警：实时监控线程池状态，异常时自动告警
 * 3. 参数优化：根据历史数据自动推荐最优参数配置
 * 4. 多环境管理：支持开发、测试、生产环境的不同配置
 * <p>
 *
 * @author zhangjlk
 * @date 2025/12/31 上午10:25
 */
@Configuration
public class Hippo4jThreadPoolConfiguration {

    /**
     * 选座线程池执行器
     * <p>
     * 用途：用于处理选座相关的异步任务，如：
     * - 异步查询座位信息
     * - 异步验证座位可用性
     * - 异步更新座位状态
     * <p>
     * 线程池配置说明：
     * <p>
     * 1. 核心线程数（corePoolSize = 24）：
     *    - 线程池中始终保持的线程数量
     *    - 即使没有任务执行，这些线程也不会被销毁
     *    - 24个核心线程可以同时处理24个选座请求
     * <p>
     * 2. 最大线程数（maximumPoolSize = 36）：
     *    - 线程池允许的最大线程数量
     *    - 当任务数量超过核心线程数且队列已满时，会创建新线程
     *    - 最多可以同时处理36个选座请求
     * <p>
     * 3. 工作队列（SYNCHRONOUS_QUEUE）：
     *    - 同步队列，容量为0，不存储任务
     *    - 任务提交后，如果没有空闲线程，会立即创建新线程执行
     *    - 适用于需要快速响应的场景，避免任务在队列中等待
     *    - 缺点：如果任务提交速度超过处理速度，会快速创建大量线程
     * <p>
     * 4. 核心线程超时（allowCoreThreadTimeOut = true）：
     *    - 允许核心线程在空闲时被回收
     *    - 当线程空闲时间超过keepAliveTime时，会被销毁
     *    - 适用于流量波动较大的场景，可以节省资源
     * <p>
     * 5. 线程存活时间（keepAliveTime = 60分钟）：
     *    - 非核心线程的空闲存活时间
    *    - 超过这个时间且线程数大于核心线程数时，线程会被回收
     *    - 60分钟是一个较长的存活时间，适合流量波动较大的场景
     * <p>
     * 6. 拒绝策略（CallerRunsPolicy）：
     *    - 当线程池和队列都满了时，新任务由调用线程执行
     *    - 这样可以降低任务提交速度，给线程池喘息的机会
     *    - 适用于不能丢失任务的场景
     * <p>
     * 参数选择依据：
     * - 核心线程数24：根据系统CPU核心数和业务并发量确定
     * - 最大线程数36：为核心线程数的1.5倍，提供一定的弹性
     * - 同步队列：选座操作需要快速响应，不适合使用有界队列
     * - 核心线程超时：流量波动大，允许核心线程回收可以节省资源
     * <p>
     * 注意事项：
     * - 使用@DynamicThreadPool注解后，可以通过Hippo4j管理平台动态调整参数
     * - 建议在生产环境中根据实际负载情况调整参数
     * - 监控线程池的指标，及时发现问题并优化
     *
     * @return 配置好的线程池执行器
     */
    @Bean
    @DynamicThreadPool  // 标记为动态线程池，可以通过Hippo4j管理平台动态调整参数
    public ThreadPoolExecutor selectSeatThreadPoolExecutor() {
        // 线程池唯一标识，用于在Hippo4j管理平台中识别和管理
        String threadPoolId = "select-seat-thread-pool-executor";
        
        return ThreadPoolBuilder.builder()
                // 设置线程池ID，用于Hippo4j管理平台识别
                .threadPoolId(threadPoolId)
                // 设置线程工厂，用于创建线程时设置线程名称
                .threadFactory(threadPoolId)
                // 设置工作队列类型：同步队列（容量为0，不存储任务）
                // 任务提交后，如果没有空闲线程，会立即创建新线程执行
                .workQueue(BlockingQueueTypeEnum.SYNCHRONOUS_QUEUE)
                // 核心线程数：24个线程始终保持活跃
                .corePoolSize(24)
                // 最大线程数：最多可以创建36个线程
                .maximumPoolSize(36)
                // 允许核心线程超时：核心线程在空闲时可以被回收
                .allowCoreThreadTimeOut(true)
                // 线程存活时间：非核心线程空闲60分钟后被回收
                .keepAliveTime(60, TimeUnit.MINUTES)
                // 拒绝策略：当线程池和队列都满了时，由调用线程执行任务
                .rejected(new ThreadPoolExecutor.CallerRunsPolicy())
                // 启用动态线程池功能，可以通过Hippo4j管理平台动态调整参数
                .dynamicPool()
                .build();
    }
}
