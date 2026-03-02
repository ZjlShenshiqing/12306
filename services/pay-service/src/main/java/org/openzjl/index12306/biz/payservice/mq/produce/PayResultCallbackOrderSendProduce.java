/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.openzjl.index12306.biz.payservice.common.constant.PayRocketMQConstant;
import org.openzjl.index12306.biz.payservice.mq.domain.MessageWrapper;
import org.openzjl.index12306.biz.payservice.mq.event.PayResultCallbackOrderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 支付结果回调订单生产者
 * <p>
 * 该生产者用于发送支付结果回调消息到订单服务，通知订单服务更新订单状态
 * 主要功能：
 * 1. 构建支付结果回调消息
 * 2. 配置消息的主题、标签、超时时间等参数
 * 3. 确保消息发送的可靠性和唯一性
 * </p>
 *
 * @author zhangjlk
 * @date 2026/2/28 上午10:32
 */
@Slf4j
@Component
public class PayResultCallbackOrderSendProduce extends AbstractCommonSendProduceTemplate<PayResultCallbackOrderEvent>{

    /**
     * 配置环境，用于解析配置文件中的占位符
     */
    private final ConfigurableEnvironment environment;

    /**
     * 构造方法
     *
     * @param rocketMQTemplate RocketMQ模板，用于发送消息
     * @param environment 配置环境，用于解析配置文件中的占位符
     */
    public PayResultCallbackOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    /**
     * 构建基础发送扩展参数
     * <p>
     * 该方法会：
     * 1. 设置事件名称为"支付结果回调订单"
     * 2. 使用订单号作为消息键
     * 3. 从配置环境中解析支付全局主题和支付结果回调标签
     * 4. 设置消息发送超时时间为2000毫秒
     * </p>
     *
     * @param messageEvent 支付结果回调订单事件
     * @return 基础发送扩展参数DTO
     */
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(PayResultCallbackOrderEvent messageEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("支付结果回调订单") // 设置事件名称
                .keys(messageEvent.getOrderSn()) // 使用订单号作为消息键
                .topic(environment.resolvePlaceholders(PayRocketMQConstant.PAY_GLOBAL_TOPIC_KEY)) // 解析支付全局主题
                .tag(environment.resolvePlaceholders(PayRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY)) // 解析支付结果回调标签
                .sentTimeout(2000L) // 设置消息发送超时时间为2000毫秒
                .build();
    }

    /**
     * 构建消息
     * <p>
     * 该方法会：
     * 1. 确保消息键不为空，为空时生成UUID作为消息键
     * 2. 构建消息包装器，包含消息键和支付结果回调订单事件
     * 3. 设置消息头，包括消息键和标签
     * </p>
     *
     * @param messageSendEvent 支付结果回调订单事件
     * @param requestParam 基础发送扩展参数
     * @return 构建好的消息
     */
    @Override
    protected Message<?> buildMessage(PayResultCallbackOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        // 确保消息键不为空，为空时生成UUID作为消息键
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        
        // 构建消息，包含消息包装器和消息头
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent)) // 构建消息包装器
                .setHeader(MessageConst.PROPERTY_KEYS, keys) // 设置消息键
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag()) // 设置消息标签
                .build();
    }
}
