package org.openzjl.index12306.framework.starter.common.threadpool.proxy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.framework.starter.common.threadpool.build.ThreadPoolBuilder;
import org.openzjl.index12306.framework.starter.common.toolkit.ThreadUtil;

import java.lang.reflect.Proxy;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 拒绝策略代理工具类
 * @author zhangjlk
 * @date 2025/9/18 10:37
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RejectedProxyUtil {

    /**
     * 创建一个“带监控功能”的拒绝策略代理对象
     *
     * 当线程池无法处理新任务时（如队列满、线程达上限），会触发拒绝策略。
     * 此方法通过 JDK 动态代理，对原始的拒绝策略进行增强，
     * 在执行原有逻辑的同时，统计被拒绝的任务数量。
     *
     * @param rejectedExecutionHandler 原始的拒绝策略（如 AbortPolicy、CallerRunsPolicy 等）
     * @param rejectedNum              用于记录被拒绝任务总数的原子变量（线程安全）
     * @return                         一个代理后的拒绝策略处理器，具备计数能力
     */
    public static RejectedExecutionHandler createProxy(RejectedExecutionHandler rejectedExecutionHandler, AtomicLong rejectedNum) {
        // 使用 JDK 动态代理生成一个 RejectedExecutionHandler 的代理实例
        return (RejectedExecutionHandler) Proxy
                .newProxyInstance(
                        // 1. 类加载器：使用原对象的类加载器
                        rejectedExecutionHandler.getClass().getClassLoader(),
                        // 2. 要实现的接口：必须是接口数组，这里只有一个 RejectedExecutionHandler 接口
                        new Class[]{RejectedExecutionHandler.class},
                        // 3. 方法调用处理器：当代理对象的方法被调用时，由这个 InvocationHandler 来决定如何处理
                        new RejectedProxyInvocationHandler(rejectedExecutionHandler, rejectedNum)
                );
    }

    /**
     * 测试线程动态代理执行程序
     */
    public static void main(String[] args) {
        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(1,3,1024, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));

        ThreadPoolExecutor.AbortPolicy abortPolicy = new ThreadPoolExecutor.AbortPolicy();
        AtomicLong rejectedNum = new AtomicLong();
        RejectedExecutionHandler proxyRejectedExecutionHandler = RejectedProxyUtil.createProxy(abortPolicy, rejectedNum);

        threadPoolExecutor.setRejectedExecutionHandler(proxyRejectedExecutionHandler);
        for (int i = 0; i < 5; i++) {
            try {
                threadPoolExecutor.execute(() -> ThreadUtil.sleep(100000L));
            } catch (Exception ignore) {
                ignore.printStackTrace();
            }
        }

        System.out.println("================== 线程池拒绝策略执行次数：" + rejectedNum.get());
    }
}
