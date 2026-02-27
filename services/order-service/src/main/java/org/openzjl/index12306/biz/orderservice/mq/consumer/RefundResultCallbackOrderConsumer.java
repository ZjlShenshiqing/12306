package org.openzjl.index12306.biz.orderservice.mq.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.openzjl.index12306.biz.orderservice.common.constant.OrderRocketMQConstant;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dto.domain.OrderItemStatusReversalDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.mq.domain.MessageWrapper;
import org.openzjl.index12306.biz.orderservice.mq.event.RefundResultCallBackOrderEvent;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 退款结果回调订单消费者
 * <p>
 * 该消费者用于监听支付服务发送的退款结果回调消息，处理订单和订单项的状态更新
 * 主要功能：
 * 1. 接收退款结果回调消息
 * 2. 处理部分退款和全额退款的情况
 * 3. 更新订单和订单项的状态
 * 4. 保证消息处理的幂等性
 * </p>
 *
 * @author zhangjlk
 * @date 2026/2/26 下午3:54
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY, // 支付全局消息主题
        selectorExpression = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY, // 退款结果回调消息标签
        consumerGroup = OrderRocketMQConstant.REFUND_RESULT_CALLBACK_ORDER_CG_KEY // 退款结果回调订单消费者组
)
public class RefundResultCallbackOrderConsumer implements RocketMQListener<MessageWrapper<RefundResultCallBackOrderEvent>> {

    /**
     * 订单项服务，用于处理订单项状态更新
     */
    private final OrderItemService orderItemService;

    /**
     * 处理退款结果回调消息
     * <p>
     * 该方法会：
     * 1. 确保消息处理的幂等性，避免重复处理
     * 2. 在事务中执行退款处理逻辑
     * 3. 根据退款类型（部分退款或全额退款）更新订单和订单项状态
     * 4. 处理部分退款的票务详情
     * </p>
     *
     * @param message 退款结果回调消息包装对象，包含退款结果详情
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-order:refund_result_callback:", // 幂等性键前缀
            key = "#message.getKeys()+'_'+#message.hashCode()", // 幂等性键，使用消息键和哈希值
            type = IdempotentTypeEnum.SPEL, // 键类型为SpEL表达式
            scene = IdempotentSceneEnum.MQ, // 幂等性场景为消息队列
            keyTimeout = 7200L // 键过期时间，7200秒（2小时）
    )
    @Transactional(rollbackFor = Exception.class) // 事务注解，遇到任何异常都回滚
    @Override
    public void onMessage(MessageWrapper<RefundResultCallBackOrderEvent> message) {
        // 从消息包装对象中获取退款结果回调事件
        RefundResultCallBackOrderEvent refundResultCallbackOrderEvent = message.getMessage();
        
        // 获取退款状态码
        Integer status = refundResultCallbackOrderEvent.getRefundTypeEnum().getCode();
        
        // 获取订单号
        String orderSn = refundResultCallbackOrderEvent.getOrderSn();
        
        // 构建订单项DO列表
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        
        // 获取部分退款的票务详情列表
        List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList = refundResultCallbackOrderEvent.getPartialRefundTicketDetailList();
        
        // 将部分退款的票务详情转换为订单项DO
        partialRefundTicketDetailList.forEach(partial -> {
            OrderItemDO orderItemDO = new OrderItemDO();
            BeanUtil.convert(partial, orderItemDO); // 使用BeanUtil进行对象转换
            orderItemDOList.add(orderItemDO);
        });
        
        // 处理部分退款情况
        if (status.equals(OrderStatusEnum.PARTIAL_REFUND.getStatus())) {
            // 构建部分退款的订单项状态反转DTO
            OrderItemStatusReversalDTO partialRefundOrderItemStatusReversalDTO = OrderItemStatusReversalDTO.builder()
                    .orderSn(orderSn) // 设置订单号
                    .orderStatus(OrderStatusEnum.PARTIAL_REFUND.getStatus()) // 设置订单状态为部分退款
                    .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus()) // 设置订单项状态为已退款
                    .orderItemDOList(orderItemDOList) // 设置订单项DO列表
                    .build();
            
            // 执行订单项状态反转，更新订单项状态
            orderItemService.orderItemStatusReversal(partialRefundOrderItemStatusReversalDTO);
        }
        // 处理全额退款情况
        else if (status.equals(OrderStatusEnum.FULL_REFUND.getStatus())) {
            // 构建全额退款的订单项状态反转DTO
            OrderItemStatusReversalDTO fullRefundOrderItemStatusReversalDTO = OrderItemStatusReversalDTO.builder()
                    .orderSn(orderSn) // 设置订单号
                    .orderStatus(OrderStatusEnum.FULL_REFUND.getStatus()) // 设置订单状态为全额退款
                    .orderItemStatus(OrderItemStatusEnum.REFUNDED.getStatus()) // 设置订单项状态为已退款
                    .orderItemDOList(orderItemDOList) // 设置订单项DO列表
                    .build();
            
            // 执行订单项状态反转，更新订单项状态
            orderItemService.orderItemStatusReversal(fullRefundOrderItemStatusReversalDTO);
        }
    }
}
