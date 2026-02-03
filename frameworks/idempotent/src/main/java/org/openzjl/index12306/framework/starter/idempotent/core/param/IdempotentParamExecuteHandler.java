/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.core.param;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.idempotent.core.IdempotentContext;
import org.openzjl.index12306.framework.starter.idempotent.core.AbstractIdempotentExecuteHandler;
import org.openzjl.index12306.framework.starter.idempotent.core.IdempotentParamWrapper;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 基于方法参数验证请求的幂等性
 *
 * 把方法的参数（如订单ID、用户ID）拼接成一个唯一标识，作为幂等 Key 去 Redis 中判断是否已处理过
 *
 * @author zhangjlk
 * @date 2025/10/6 11:57
 */
@RequiredArgsConstructor
public final class IdempotentParamExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentParamService {

    private final RedissonClient redissonClient;

    private final static String LOCK = "lock:param:restAPI";

    /**
     * 构建幂等参数包装器 —— 核心：生成唯一的 lockKey
     *
     * Key 格式：
     *   idempotent:path:{servletPath}:currentUserId:{userId}:md5:{argsMD5}
     *
     * 示例：
     *   请求路径：/order/create
     *   用户ID：123
     *   参数MD5：a1b2c3d4e5f6...
     *   → Key: idempotent:path:/order/create:currentUserId:123:md5:a1b2c3d4e5f6...
     *
     * @param joinPoint AOP 方法连接点
     * @return 幂等参数包装器
     */
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        String lockKey = String.format("idempotent:path:%s:currentUserId:%s:md5:%s", getServletPath(), getCurrentUserId(), calcArgsMD5(joinPoint));
        return IdempotentParamWrapper.builder().lockKey(lockKey).proceedingJoinPoint(joinPoint).build();
    }

    /**
     * 获取当前请求的 Servlet 路径（即：URL 中除去 context-path 和 query-string 的部分）
     *
     * 示例：
     * - 请求 URL: http://localhost:8080/api/user/list?name=张三
     * - 返回值: /user/list
     */
    private String getServletPath() {
        // 1. 从 Spring 的 RequestContextHolder 中获取当前请求的属性对象
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 2. 如果没有请求上下文（比如在非 Web 环境中），返回空字符串或抛异常
        if (requestAttributes == null) {
            return ""; // 或 throw new IllegalStateException("No request context");
        }

        // 3. 获取 HttpServletRequest 对象
        HttpServletRequest request = requestAttributes.getRequest();

        // 4. 获取 Servlet 路径（即：/user/list）
        return request.getServletPath();
    }

    /**
     * 获取当前登录用户的 ID
     *
     * 说明：
     * - 从 UserContext 中获取用户 ID
     * - 如果为空或空白，说明用户未登录 → 抛出 ClientException 提示“请登录”
     * - 返回非空用户 ID，供后续业务逻辑使用
     */
    private String getCurrentUserId() {
        String userId = UserContext.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new ClientException("用户ID获取失败，请登录");
        }
        return userId;
    }


    /**
     * 计算方法参数的 MD5 哈希值（作为唯一标识）
     *
     * 这个方法的作用是：把 AOP 切面中方法的“所有参数”序列化成 JSON 字符串，再计算 MD5 哈希值，生成一个唯一的“参数指纹”
     *
     * 示例：
     * - 参数: [123, "张三", true]
     * - JSON: "[123,\"张三\",true]"
     * - MD5: "a1b2c3d4e5f6..."
     */
    private String calcArgsMD5(ProceedingJoinPoint joinPoint) {
        // 1. 获取方法的所有参数值（Object[] 类型）
        Object[] args = joinPoint.getArgs();

        // 2. 将参数数组序列化为 JSON 字节流（使用 Hutool 的 JSON 工具）
        byte[] jsonBytes = JSON.toJSONBytes(args);

        // 3. 计算 MD5 哈希值，并转为十六进制字符串
        return DigestUtil.md5Hex(jsonBytes);
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 1. 从包装器中取出幂等锁的 Key（如 "idempotent:order:123"）
        String lockKey = wrapper.getLockKey();

        // 2. 用 Redisson 获取一个分布式锁对象（基于 Redis 实现）
        RLock lock = redissonClient.getLock(lockKey);

        // 3. 尝试获取锁，如果获取失败（说明别人正在处理），抛异常
        if (!lock.tryLock()) {
            throw new ClientException(wrapper.getIdempotent().message());
        }

        // 4. 把锁对象存入当前线程上下文，供后续释放使用
        IdempotentContext.put(LOCK, lock);
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

    @Override
    public void exceptionProcessing() {
        postProcessing();
    }
}
