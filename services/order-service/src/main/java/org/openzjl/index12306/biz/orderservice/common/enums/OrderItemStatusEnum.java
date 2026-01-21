package org.openzjl.index12306.biz.orderservice.common.enums;

import cn.crane4j.annotation.ContainerEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单明细状态枚举
 *
 * @author zhangjlk
 * @date 2026/1/21 11:48
 */
@RequiredArgsConstructor
@ContainerEnum(namespace = "OrderItemStatusEnum", key = "status", value = "statusName")
public enum OrderItemStatusEnum {

    /**
     * 待支付：用户选好车票下单，但还未付款的状态
     */
    PENDING_PAYMENT(0, "待支付"),

    /**
     * 已支付：用户支付订单费用
     */
    ALREADY_PAID(10, "已支付"),

    /**
     * 已完成：用户车票已过上站时间，订单完成
     */
    ALREADY_PULL_IN(20, "已进站"),

    /**
     * 已取消：用户选好车票下单，未支付状态下取消订单
     */
    CLOSED(30, "已取消"),

    /**
     * 已退票
     */
    REFUNDED(40, "已退票"),

    /**
     * 已改签
     */
    RESCHEDULED(50, "已改签");

    @Getter
    private final int status;

    @Getter
    private final String statusName;
}
