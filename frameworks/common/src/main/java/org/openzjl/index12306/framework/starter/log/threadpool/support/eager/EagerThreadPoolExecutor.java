package org.openzjl.index12306.framework.starter.log.threadpool.support.eager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 快速消费线程池
 *
 * EagerThreadPoolExecutor 是“线程池”本身，负责管理线程的创建与执行；
 * TaskQueue 是它使用的“任务队列”，通过自定义入队逻辑来影响线程池的行为。
 *
 * @author zhangjlk
 * @date 2025/9/18 10:37
 */
public class EagerThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * 构造一个“急切型”线程池（EagerThreadPoolExecutor）。
     *
     * 与标准 ThreadPoolExecutor 的“懒启动”策略不同，该线程池会更积极地创建线程，
     * 尽量避免将任务放入队列等待，从而提升高并发场景下的响应速度。
     *
     * 实现原理依赖于自定义的任务队列 {@link TaskQueue}，它通过重写 offer 方法来控制：
     * 当线程池中当前线程数小于最大线程数时，拒绝入队 → 触发线程池创建新线程（即使是核心线程），
     * 从而实现“急切”执行任务的效果。
     *
     * @param corePoolSize      核心线程数量，这些线程会保持存活（除非设置了 allowCoreThreadTimeOut）
     * @param maximumPoolSize   最大线程数量，表示线程池允许创建的最大线程总数
     * @param keepAliveTime     非核心线程在空闲时的存活时间
     * @param timeUnit          时间单位（如 TimeUnit.SECONDS）
     * @param workQueue         任务队列，必须是 {@link TaskQueue} 类型，且需绑定当前线程池实例才能生效
     * @param threadFactory     线程工厂，用于创建新线程（可自定义线程名称、优先级等）
     * @param handler           当线程池饱和（线程数达到上限且队列满）时的拒绝策略
     */
    public EagerThreadPoolExecutor(int corePoolSize,
                                   int maximumPoolSize,
                                   long keepAliveTime,
                                   TimeUnit timeUnit,
                                   TaskQueue<Runnable> workQueue,
                                   ThreadFactory threadFactory,
                                   RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, timeUnit, workQueue, threadFactory, handler);
    }

    /**
     * 用于统计当前已提交但尚未完成执行的任务总数。
     *
     * 注意：此计数器的增加操作（increment）通常发生在任务被提交到线程池时，
     *      而减少操作在此处的 afterExecute 回调中完成，确保每个任务只被加一次、减一次。
     */
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    /**
     * 获取当前已提交但尚未执行完毕的任务数量。
     *
     * 返回值表示：已经调用过 increment 操作但还未在 afterExecute 中 decrement 的任务数。
     *
     * @return 当前未完成的任务总数
     */
    public int getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }

    /**
     * 任务执行完成后的回调方法（钩子方法）。
     *
     * 此方法由 ThreadPoolExecutor 在每个任务执行结束后自动调用（无论正常结束还是抛出异常）。
     * 在这里将已提交任务计数器减一，表示该任务已完成处理。
     *
     * - 该方法不会对排队中的任务调用，只有真正被执行过的任务才会触发；
     * - 必须保证在任务提交时已对 submittedTaskCount 执行了 increment 操作（通常在 execute 方法中）；
     *
     * @param r 执行完成的任务
     * @param t 任务执行过程中抛出的异常（null 表示正常完成）
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        submittedTaskCount.decrementAndGet();
    }

    @Override
    public void execute(Runnable command) {
        submittedTaskCount.incrementAndGet();
        try {
            // 正常走线程池的流程去执行任务
            super.execute(command);
        } catch (RejectedExecutionException ex) {
            // 给线程池拒绝了
            // 1. 有空的核心线程？直接交给它。
            // 2. 核心都忙？尝试把任务放进队列等待。
            // 3. 队列满了？创建非核心线程来处理。
            // 4. 连非核心线程也不能再创建了？那就抛出“拒绝”异常。
            TaskQueue taskQueue = (TaskQueue) super.getQueue();
            try {
                if (!taskQueue.retryOffer(command, 0, TimeUnit.MICROSECONDS)) {
                    // 把之前多加的计数减回去（防止计数错误导致后续判断出错）
                    submittedTaskCount.decrementAndGet();
                    throw new RejectedExecutionException("消息队列容量已满", ex);
                }
            } catch (InterruptedException iex) {
                submittedTaskCount.decrementAndGet();
                throw new RejectedExecutionException(iex);
            }
        } catch (Exception ex) {
            submittedTaskCount.decrementAndGet();
            throw ex;
        }
    }
}
