/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.core.spel;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.core.*;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentMQConsumeStatusEnum;
import org.openzjl.index12306.framework.starter.idempotent.toolkit.LogUtil;
import org.openzjl.index12306.framework.starter.idempotent.toolkit.SpELUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于SpEL方法验证请求的幂等性，适用于MQ的场景
 *
 * @author zhangjlk
 * @date 2025/10/8 20:47
 */
@RequiredArgsConstructor
public final class IdempotentSpELByMQExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {

    /**
     * 超时时间
     */
    private final static int TIMEOUT = 600;

    /**
     * 定义一个用作缓存键前缀的常量字符串。
     * 使用 "wrapper:spEL:MQ" 这样的命名空间可以有效避免与其他业务或应用在同一个缓存实例中发生键冲突。
     */
    private final static String WRAPPER = "wrapper:spEL:MQ";

    /**
     * 定义Lua脚本文件的路径
     */
    private final static String LUA_SCRIPT_SET_IF_ABSENT_AND_GET_PATH = "lua/set_if_absent_and_get.lua";

    /**
     * 分布式缓存
     */
    private final DistributedCache distributedCache;

    @Override
    @SneakyThrows
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        // 解析 SpEL 表达式，生成最终的 key
        String key = (String) SpELUtil.parseKey(idempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());

        // 构建并返回一个包装对象 IdempotentParamWrapper
        // 这个对象封装了幂等处理后续步骤所需的所有信息。
        return IdempotentParamWrapper.builder()
                .lockKey(key) // 封装上一步计算出的、用于分布式锁的唯一 key
                .proceedingJoinPoint(joinPoint) // 封装原始的 joinPoint，以便后续在检查通过后可以调用 joinPoint.proceed() 来执行原始方法
                .build();
    }

    /**
     * 幂等性检查的核心处理方法.
     * <p>
     * 该方法实现了幂等性检查的关键逻辑：尝试在分布式缓存中原子性地标记一个唯一请求。
     * <ul>
     * <li><b>成功:</b> 如果标记成功（说明是首次处理），则将上下文信息存入 {@link IdempotentContext}，以便后续环节（如业务执行完毕后更新状态）使用，并允许业务逻辑继续执行。</li>
     * <li><b>失败:</b> 如果标记失败（说明是重复请求），则记录警告日志，并抛出 {@link RepeatConsumptionException} 异常，从而中断业务逻辑的执行。</li>
     * </ul>
     *
     * @param wrapper 包含了幂等操作所需全部参数的包装对象, 如幂等注解、SpEL解析后的key等.
     * @throws RepeatConsumptionException 当检测到重复消费时抛出.
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 1. 组装最终的、全局唯一的缓存 Key.
        // 格式为：注解中定义的前缀 + SpEL表达式动态计算出的Key.
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();

        // 2. 调用核心原子操作：如果 Key 不存在，则设置值为 "CONSUMING" (消费中)，并返回 null.
        //    如果 Key 已存在，则不进行任何操作，并返回该 Key 当前的值 (如 "CONSUMING" 或 "SUCCESS").
        String absentAndGet = this.setIfAbsentAndGet(uniqueKey,
                IdempotentMQConsumeStatusEnum.CONSUMING.getCode(),
                TIMEOUT,
                TimeUnit.SECONDS);

        // 3. 判断是否为重复消费.
        //    根据我们对 setIfAbsentAndGet 的约定，只有在重复消费时，返回值才不为 null。
        if (Objects.nonNull(absentAndGet)) {
            // 3.1. 检测已存在的消费记录是否为异常状态，如果返回的是消费中，就是失败的
            boolean error = IdempotentMQConsumeStatusEnum.isError(absentAndGet);

            // 3.2. 记录警告日志，明确告知是重复消费，并提示当前记录的状态.
            //      - 如果是 error 状态，提示 "等待客户端延迟消费"，暗示后续会有重试机制.
            //      - 如果是其他状态 (如 COMPLETED)，提示 "状态是已完成".
            LogUtil.getLog(wrapper.getProceedingJoinPoint()).warn("[{}] MQ repeated consumption, {}.",
                    uniqueKey, error ? "Wait for the client to delay consumption" : "Status is completed");

            // 3.3. 抛出自定义的“重复消费异常”，中断当前方法的执行流程.
            //      AOP 切面会捕获此异常，从而阻止其后真正的业务逻辑被调用。
            throw new RepeatConsumptionException(error);
        }

        // 4. (仅在首次消费时执行) 将 Wrapper 对象存入 IdempotentContext.
        IdempotentContext.put(WRAPPER, wrapper);
    }

    /**
     * 通过 Lua 脚本执行原子的 "SET if Absent and GET" (如果不存在则设置，并返回当前值) 操作.
     *
     * <p>
     * <b>方法契约:</b>
     * 此方法旨在原子性地完成一个锁获取或状态标记的动作。
     * <ul>
     * <li>若 {@code key} 不存在，则设置 {@code value} 并设定过期时间，方法返回 {@code value}。</li>
     * <li>若 {@code key} 已存在，则不做任何操作，方法返回该 {@code key} 已有的旧值。</li>
     * </ul>
     * 这种原子性保证对于实现分布式锁和幂等性至关重要，它彻底避免了 "check-then-act" 模式下的竞态条件。
     *
     * @param key      缓存键，业务唯一标识.
     * @param value    当键不存在时，需要设置的值 (通常表示状态，如 "PROCESSING").
     * @param timeout  过期时间数值.
     * @param timeUnit 过期时间的单位 (例如, TimeUnit.SECONDS).
     * @return           操作后 key 对应的最终值。可能是新设置的 {@code value}，也可能是已存在的旧值。
     */
    public String setIfAbsentAndGet(String key, String value, long timeout, TimeUnit timeUnit) {
        // 1. 准备 Redis Lua 脚本对象
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_SCRIPT_SET_IF_ABSENT_AND_GET_PATH)));
        redisScript.setResultType(String.class);

        // 2. 统一时间单位为毫秒，以适配 Redis 的 PX 参数
        long timeoutMillis = timeUnit.toMillis(timeout);

        // 3. 获取底层的 RedisTemplate 客户端并执行脚本
        // 参数说明:
        // - redisScript: 待执行的脚本
        // - List.of(key): 传入脚本的 KEYS 数组，脚本中通过 KEYS[1] 访问
        // - value, String.valueOf(timeoutMillis): 传入脚本的 ARGV 数组，脚本中通过 ARGV[1], ARGV[2] 访问
        return ((StringRedisTemplate) distributedCache.getInstance()).execute(
                redisScript,
                List.of(key),
                value,
                String.valueOf(timeoutMillis)
        );
    }

    /**
     * 幂等性检查的异常处理流程
     * <p>
     * 此方法通常在 AOP 的 {@code @AfterThrowing} 通知中被调用。
     * 其核心职责是：当业务逻辑执行失败并抛出异常时，清理掉在幂等检查初始阶段设置的
     * "消费中" 状态令牌。这是一种 "快速失败" 策略，旨在允许消息能够被快速重试。
     */
    @Override
    public void exceptionProcessing() {
        // 1. 从 ThreadLocal 上下文中获取当前请求的幂等参数包装器.
        //    只有在 handler 方法成功执行（即首次消费）后，这个上下文中才会有值。
        IdempotentParamWrapper wrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);

        // 2. 空指针判断，确保上下文存在，增加代码健壮性.
        if (wrapper != null) {
            // 3. 重新构建出与 handler 方法中完全一致的唯一幂等键.
            Idempotent idempotent = wrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + wrapper.getLockKey();

            try {
                // 4. 尝试从分布式缓存（如Redis）中删除该幂等键.
                //    删除成功后，后续的重试请求将能够重新获取令牌并执行。
                distributedCache.delete(uniqueKey);
            } catch (Throwable ex) {
                // 5. 兜底异常处理：如果连删除操作本身都失败了，记录一条严重错误日志.
                //    这种情况通常意味着缓存服务出现问题，需要运维人员关注。
                LogUtil.getLog(wrapper.getProceedingJoinPoint()).error("[{}] Failed to del MQ anti-heavy token.", uniqueKey);
            }
        }
    }

    /**
     * 幂等性检查的后置处理方法（成功路径）.
     * <p>
     * 此方法在主业务逻辑成功执行后被调用，负责将幂等令牌的状态更新为最终完成态，
     * 以此来正式关闭本次幂等操作的生命周期。
     */
    @Override
    public void postProcessing() {
        // 1. 从 ThreadLocal 上下文中获取当前请求的幂等参数包装器.
        IdempotentParamWrapper idempotentParamWrapper = (IdempotentParamWrapper) IdempotentContext.getKey(WRAPPER);

        // 2. 确保上下文存在，这是执行后续逻辑的前提.
        if (idempotentParamWrapper != null) {
            Idempotent idempotent = idempotentParamWrapper.getIdempotent();
            String uniqueKey = idempotent.uniqueKeyPrefix() + idempotentParamWrapper.getLockKey();
            try {
                // 3. 核心步骤：将幂等键的状态更新为“已消费”，并刷新过期时间。
                //    这是幂等流程的“收尾”动作，用一个明确的最终状态来标记业务已成功处理完毕。
                //    即使key的过期时间很长，这个最终状态也为日后排查问题提供了确切的证据。
                distributedCache.put(
                        uniqueKey,
                        IdempotentMQConsumeStatusEnum.CONSUMED.getCode(), // 修正后的正确状态
                        idempotent.keyTimeout(),
                        TimeUnit.SECONDS
                );
            } catch (Throwable ex) {
                // 4. 兜底异常处理：如果更新最终状态失败，记录严重错误日志.
                //    这种情况需要特别关注，因为它可能导致后续重复请求被误判。
                LogUtil.getLog(idempotentParamWrapper.getProceedingJoinPoint()).error("[{}] Failed to set MQ anti-heavy token final status.", uniqueKey, ex);
            }
        }
    }
}
