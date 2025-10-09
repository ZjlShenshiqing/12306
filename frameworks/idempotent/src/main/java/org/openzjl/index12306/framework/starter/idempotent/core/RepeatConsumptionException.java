package org.openzjl.index12306.framework.starter.idempotent.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 【重复消费异常】—— 专用于消息队列消费场景的业务异常
 *
 * 使用场景：
 * - 当消费者收到一条消息，发现它已经被处理过（幂等控制）
 * - 但不确定上次是否真正执行成功 → 抛出此异常，让 MQ 重新投递（进入重试队列）
 * - 如果确定已成功 → 直接返回成功，不抛异常
 *
 * 注意：
 * 此类异常通常配合 AOP 或幂等拦截器使用，用于控制消息重试行为。
 *
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：12306）获取项目资料
 */
@RequiredArgsConstructor
public class RepeatConsumptionException extends RuntimeException {

    /**
     * 是否为“错误状态” —— 即是否需要触发重试机制
     *
     * 说明：
     * 在幂等控制中，有以下两种典型场景：
     *
     * 消息正在处理中（比如锁住了），但不确定上一次是否执行成功
     * → 设置 error = true → 抛出异常 → RocketMQ 会把消息放入 RETRY TOPIC 重试
     *
     * 消息已经处理成功（Redis 中有记录），且结果一致
     * → 设置 error = false → 不抛异常 → 消费者正常返回，MQ 认为消费成功
     *
     * 然后再在业务那边设置错误返回就行
     */
    @Getter
    private final Boolean error;
}
