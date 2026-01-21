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
}
