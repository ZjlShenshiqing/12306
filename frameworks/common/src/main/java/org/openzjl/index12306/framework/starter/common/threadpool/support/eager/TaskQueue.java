package org.openzjl.index12306.framework.starter.common.threadpool.support.eager;

import lombok.Setter;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 快速消费任务队列
 *
 * @author zhangjlk
 * @date 2025/9/18 10:37
 */
public class TaskQueue<R extends Runnable> extends LinkedBlockingQueue<Runnable> {

    @Setter
    private EagerThreadPoolExecutor executor;

    public TaskQueue(int capacity) {
        super(capacity);
    }

    @Override
    public boolean offer(Runnable runnable) {
        // 当前线程池里正在运行的工作线程总数
        int currentPoolThreadSize = executor.getPoolSize();

        // 如果核心线程正在空闲状态，任务将加入阻塞队列，并由核心线程处理任务
        if (executor.getSubmittedTaskCount() < currentPoolThreadSize) {
            return super.offer(runnable);
        }

        // 如果线程池数量小于最大线程数，返回false，这样会创建非核心线程
        if (currentPoolThreadSize < executor.getMaximumPoolSize()) {
            return false;
        }

        // 当线程数已达上限（不能再创建新线程），只能将任务加入队列等待处理
        return super.offer(runnable);
    }

    /**
     * 尝试将任务提交到工作队列中，支持超时重试机制。
     * 如果线程池已关闭，则直接拒绝任务；否则在指定时间内等待入队。
     *
     * @param runnable              要提交的任务（必须是 Runnable 类型）
     * @param timeout               等待入队的最长时间
     * @param unit                  时间单位（如 TimeUnit.SECONDS、TimeUnit.MILLISECONDS）
     * @return true                 表示任务成功加入队列；false 表示超时或入队失败
     * @throws InterruptedException 如果当前线程在等待时被中断，则抛出此异常
     */
    public boolean retryOffer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        // 【安全检查】如果线程池已经调用 shutdown 或 shutdownNow，不再接受新任务
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("Executor has been shutdown");
        }

        // 【尝试入队】使用父类（阻塞队列）的带超时 offer 方法：
        // - 如果队列有空位，立即返回 true
        // - 如果队列满，则最多等待指定时间
        // - 等待期间可被中断，超时后返回 false
        return super.offer(runnable, timeout, unit);
    }
}
