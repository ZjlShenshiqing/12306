/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.core.spel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.core.AbstractIdempotentExecuteHandler;
import org.openzjl.index12306.framework.starter.idempotent.core.IdempotentAspect;
import org.openzjl.index12306.framework.starter.idempotent.core.IdempotentContext;
import org.openzjl.index12306.framework.starter.idempotent.core.IdempotentParamWrapper;
import org.openzjl.index12306.framework.starter.idempotent.toolkit.SpELUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 基于SpEL方法验证请求参数的幂等性
 * 适用于RestAPI场景
 *
 * @author zhangjlk
 * @date 2025/10/8 20:09
 */
@RequiredArgsConstructor
public final class IdempotentSpELByRestAPIExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {

    private final RedissonClient redissonClient;

    private final static String LOCK = "lock:spEL:restAPI";

    /**
     * 构建幂等性检查所需的核心参数包装器.
     *
     * <p>
     * 此方法作为幂等性切面（AOP）的前置环节，在目标方法执行前被调用。其核心职责是解析
     * {@link Idempotent} 注解中定义的 SpEL 表达式，结合目标方法的运行时参数，动态生成
     * 全局唯一的幂等键（idempotent key）。
     *
     * <p>
     * 作为模板方法（Template Method）设计模式的一部分，它为后续的幂等处理 {@code handler}
     * 准备了包含了所有必要上下文的 {@link IdempotentParamWrapper} 对象。
     *
     * @param joinPoint AOP 代理的目标方法调用连接点，提供了对方法签名、注解及运行时参数的访问能力。
     * @return 包含了计算后的唯一幂等键和原始 {@code ProceedingJoinPoint} 的参数包装对象。
     * @throws Throwable 当 SpEL 解析或反射操作失败时，通过 {@code @SneakyThrows} 向上抛出异常。
     */
    @Override
    @SneakyThrows
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        String key = (String) SpELUtil.parseKey(idempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        return IdempotentParamWrapper.builder().lockKey(key).proceedingJoinPoint(joinPoint).build();
    }


    /**
     * 幂等性前置处理器核心实现
     *
     * <p>
     * 此方法作为幂等性检查流程的关键入口，在业务逻辑执行前进行拦截。其核心职责是利用
     * 分布式锁（Redisson实现）对具有相同业务标识的请求进行并发控制，确保在分布式环境下，
     * 同一业务操作在同一时刻只有一个实例能够执行。
     *
     * <p>
     * 它的工作流程为：首先根据业务参数动态生成一个唯一的锁键（uniqueKey），然后尝试
     * 以非阻塞方式获取该锁。若获取成功，则将锁对象存入线程上下文，以便在后续流程中释放；
     * 若获取失败，则判定为并发重复请求，并立即抛出异常中断执行。
     *
     * @param wrapper 包含了幂等操作所需全部参数的包装对象.
     * @throws ClientException 当无法获取该业务操作的分布式锁时（即已有另一个相同操作正在执行）抛出.
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 1. 根据注解前缀和动态SpEL表达式的结果，组装成一个与具体业务操作绑定的唯一键.
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();

        // 2. 使用上一步生成的 uniqueKey 从 Redisson 获取一个分布式锁实例.
        //    这确保了锁的粒度是针对每一次具体的业务操作（例如，只锁订单 "12345"），而非全局锁.
        RLock lock = redissonClient.getLock(uniqueKey);

        // 3. 以非阻塞方式尝试获取锁 (tryLock).
        //    - 如果获取成功，返回 true，当前线程持有该锁.
        //    - 如果锁已被其他线程/进程持有，立即返回 false，不进行等待.
        if (!lock.tryLock()) {
            // 4. 获取锁失败，证明有完全相同的操作正在进行中.
            //    立即抛出客户端异常，拒绝本次请求，防止并发执行.
            throw new ClientException(wrapper.getIdempotent().message());
        }

        // 5. 成功获取锁后，将锁（RLock）对象本身存入线程上下文（IdempotentContext）.
        //    此举至关重要，目的是为了让 AOP 的后置通知（@After, @AfterThrowing, @AfterReturning）
        //    能够拿到这个确切的锁对象，并在业务执行完毕或出现异常时，执行 lock.unlock() 操作，
        //    从而保证锁一定会被释放，避免产生死锁。
        IdempotentContext.put(LOCK, lock);
    }

    @Override
    public void exceptionProcessing() {
        RLock lock = null;
        try {
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void postProcessing() {
        RLock lock = null;
        try {
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
