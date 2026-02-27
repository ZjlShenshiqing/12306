/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.common.constant;

/**
 * RocketMQ 订单服务常量类
 *
 * @author zhangjlk
 * @date 2026/1/21 10:16
 */
public final class OrderRocketMQConstant {

    /**
     * 订单服务相关业务Topic Key
     */
    public static final String ORDER_DELAY_CLOSE_TOPIC_KEY = "index12306_order_service_delay-close-order_topic${unique-name:}";

    /**
     * 购票服务创建订单后延时关闭业务Tag Key
     */
    public static final String ORDER_DELAY_CLOSE_TAG_KEY = "index12306_order_service_delay-close-tag-key";

    /**
     * 支付服务相关业务 Topic Key
     */
    public static final String PAY_GLOBAL_TOPIC_KEY = "index12306_pay-service_topic${unique-name:}";

    /**
     * 支付结果回调状态 Tag Key
     */
    public static final String PAY_RESULT_CALLBACK_TAG_KEY = "index12306_pay-service_pay-result-callback_tag${unique-name:}";

    /**
     * 支付结果回调订单消费者组 Key
     */
    public static final String PAY_RESULT_CALLBACK_ORDER_CG_KEY = "index12306_pay-service_pay-result-callback_cg${unique-name:}";

    /**
     * 退款结果回调订单 Tag Key
     */
    public static final String REFUND_RESULT_CALLBACK_TAG_KEY = "index12306_pay-service_refund-result-callback_tag${unique-name:}";

    /**
     * 退款结果回调订单消费者组 Key
     */
    public static final String REFUND_RESULT_CALLBACK_ORDER_CG_KEY = "index12306_pay-service_refund-result-callback_cg${unique-name:}";
}
