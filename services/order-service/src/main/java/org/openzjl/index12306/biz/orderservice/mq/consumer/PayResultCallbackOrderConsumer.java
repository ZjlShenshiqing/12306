package org.openzjl.index12306.biz.orderservice.mq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.openzjl.index12306.biz.orderservice.common.constant.OrderRocketMQConstant;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import org.openzjl.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import org.openzjl.index12306.biz.orderservice.mq.domain.MessageWrapper;
import org.openzjl.index12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import org.openzjl.index12306.biz.orderservice.service.OrderService;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付结果回调订单消费者
 * <p>
 * 该消费者用于监听支付服务发送的支付结果回调消息，处理订单状态更新和支付回调逻辑
 * 主要功能：
 * 1. 接收支付结果回调消息
 * 2. 更新订单状态为已支付
 * 3. 处理支付回调的业务逻辑
 * 4. 保证消息处理的幂等性
 * </p>
 *
 * @author zhangjlk
 * @date 2026/2/26 下午3:25
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY, // 支付全局消息主题
        selectorExpression = OrderRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY, // 支付结果回调消息标签
        consumerGroup = OrderRocketMQConstant.PAY_RESULT_CALLBACK_ORDER_CG_KEY // 支付结果回调订单消费者组
)
public class PayResultCallbackOrderConsumer implements RocketMQListener<MessageWrapper<PayResultCallbackOrderEvent>> {

    /**
     * 订单服务，用于处理订单状态更新和支付回调逻辑
     */
    private final OrderService orderService;

    /**
     * 处理支付结果回调消息
     * <p>
     * 该方法会：
     * 1. 确保消息处理的幂等性，避免重复处理
     * 2. 在事务中执行订单状态更新和支付回调逻辑
     * 3. 更新订单状态为已支付
     * 4. 处理支付回调的业务逻辑
     * </p>
     *
     * @param message 支付结果回调消息包装对象，包含支付结果详情
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-order:pay_result_callback:", // 幂等性键前缀
            key = "#message.getKeys()+'_'+#message.hashCode()", // 幂等性键，使用消息键和哈希值
            type = IdempotentTypeEnum.SPEL, // 键类型为SpEL表达式
            scene = IdempotentSceneEnum.MQ, // 幂等性场景为消息队列
            keyTimeout = 7200L // 键过期时间，7200秒（2小时）
    )
    @Transactional(rollbackFor = Exception.class) // 事务注解，遇到任何异常都回滚
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackOrderEvent> message) {
        // 从消息包装对象中获取支付结果回调事件
        PayResultCallbackOrderEvent payResultCallbackOrderEvent = message.getMessage();
        
        // 构建订单状态反转DTO，用于更新订单状态为已支付
        OrderStatusReversalDTO orderStatusReversalDTO = OrderStatusReversalDTO.builder()
                .orderSn(payResultCallbackOrderEvent.getOrderSn()) // 设置订单号
                .orderStatus(OrderStatusEnum.ALREADY_PAID.getStatus()) // 设置订单状态为已支付
                .orderItemStatus(OrderItemStatusEnum.ALREADY_PAID.getStatus()) // 设置订单项状态为已支付
                .build();
        
        // 执行订单状态反转，更新订单状态为已支付
        orderService.statusReversal(orderStatusReversalDTO);
        
        // 执行支付回调订单逻辑，处理支付成功后的业务逻辑
        orderService.payCallbackOrder(payResultCallbackOrderEvent);
    }
}
