/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

import java.util.Optional;

/**
 * MQ消息发送抽象模板类
 * <p>
 * 提供统一的RocketMQ消息发送模板，封装了消息发送的通用逻辑，
 * 采用模板方法模式，将消息发送的公共流程与具体实现分离，
 * 简化子类的开发，提高代码的可维护性和可扩展性。
 * </p>
 * 
 * <p><strong>设计理念：</strong></p>
 * <ul>
 *   <li><strong>模板方法模式：</strong>定义消息发送的骨架流程，将具体实现延迟到子类</li>
 *   <li><strong>职责分离：</strong>将消息构建与发送流程分离，每个组件只负责自己的职责</li>
 *   <li><strong>统一管理：</strong>集中处理消息发送的通用逻辑，如地址构建、日志记录、异常处理</li>
 * </ul>
 * 
 * <p><strong>使用方式：</strong></p>
 * <ol>
 *   <li>创建子类继承本抽象类，指定具体的消息事件类型 T</li>
 *   <li>实现 {@link #buildBaseSendExtendParam(Object)} 方法，构建消息发送的扩展参数</li>
 *   <li>实现 {@link #buildMessage(Object, BaseSendExtendDTO)} 方法，构建具体的消息对象</li>
 *   <li>调用 {@link #sendMessage(Object)} 方法完成消息发送</li>
 * </ol>
 * 
 * <p><strong>功能特性：</strong></p>
 * <ul>
 *   <li><strong>支持Topic和Tag的组合：</strong>自动构建目标地址，格式为 "Topic:Tag"</li>
 *   <li><strong>支持延迟消息：</strong>通过延迟级别参数控制消息的延迟发送时间</li>
 *   <li><strong>支持发送超时配置：</strong>可设置消息发送的超时时间，避免无限等待</li>
 *   <li><strong>完整的日志记录：</strong>记录发送结果和失败原因，便于问题排查</li>
 *   <li><strong>统一的异常处理：</strong>捕获并记录异常，然后重新抛出，确保调用方感知</li>
 * </ul>
 *
 * @param <T> 消息事件类型，由子类指定具体的消息事件类型
 * @author zhangjlk
 * @date 2026/1/20 11:57
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {

    /**
     * RocketMQ模板
     * <p>
     * Spring集成RocketMQ的核心模板类，提供了消息发送的各种方法，
     * 本模板类使用其同步发送方法，确保消息发送的可靠性。
     * </p>
     */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送的扩展参数
     * <p>
     * 子类必须实现此方法，根据具体的消息事件构建消息发送的扩展参数，
     * 包括事件名称、Topic、Tag、Keys、发送超时时间、延迟级别等信息。
     * </p>
     *
     * @param messageEvent 消息事件对象
     *                     <ul>
     *                       <li>包含消息发送所需的业务信息</li>
     *                       <li>由调用方传入，作为构建参数的数据源</li>
     *                     </ul>
     * @return 消息发送扩展参数对象
     *         <ul>
     *           <li>包含Topic、Tag、Keys、发送超时时间、延迟级别等信息</li>
     *           <li>作为后续消息构建和发送的依据</li>
     *         </ul>
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageEvent);

    /**
     * 构建消息对象
     * <p>
     * 子类必须实现此方法，根据消息事件和扩展参数构建Spring Message对象，
     * 负责将业务数据转换为RocketMQ可发送的消息格式。
     * </p>
     *
     * @param messageEvent 消息事件对象
     *                     <ul>
     *                       <li>包含消息的业务数据</li>
     *                       <li>作为消息体的数据源</li>
     *                     </ul>
     * @param requestParam 消息发送扩展参数
     *                     <ul>
     *                       <li>包含消息发送的配置信息</li>
     *                       <li>可用于消息头的设置</li>
     *                     </ul>
     * @return Spring Message对象
     *         <ul>
     *           <li>包含消息体和消息头</li>
     *           <li>符合Spring Messaging规范的消息格式</li>
     *           <li>将被RocketMQTemplate用于发送</li>
     *         </ul>
     */
    protected abstract Message<?> buildMessage(T messageEvent, BaseSendExtendDTO requestParam);

    /**
     * 发送消息到RocketMQ
     * <p>
     * 使用同步发送方式，等待发送结果返回，确保消息发送的可靠性。
     * 此方法是模板类的核心方法，定义了消息发送的完整流程。
     * </p>
     *
     * @param messageSendEvent 消息发送事件对象
     *                         <ul>
     *                           <li>包含消息发送所需的业务信息</li>
     *                           <li>将传递给子类的构建方法</li>
     *                         </ul>
     * @return RocketMQ发送结果
     *         <ul>
     *           <li>包含发送状态、消息ID、队列信息等</li>
     *           <li>可用于判断消息是否发送成功</li>
     *         </ul>
     * @throws Throwable 发送失败时抛出异常
     *                   <ul>
     *                     <li>可能的异常包括：网络异常、Broker异常等</li>
     *                     <li>异常会被记录日志后重新抛出，由调用方处理</li>
     *                   </ul>
     * 
     * <p><strong>发送流程：</strong></p>
     * <ol>
     *   <li><strong>构建扩展参数：</strong>调用子类实现的 {@link #buildBaseSendExtendParam(Object)} 方法</li>
     *   <li><strong>构建目标地址：</strong>根据Topic和Tag构建目标地址，格式为 "Topic:Tag" 或 "Topic"
     *   <li><strong>构建消息对象：</strong>调用子类实现的 {@link #buildMessage(Object, BaseSendExtendDTO)} 方法</li>
     *   <li><strong>同步发送消息：</strong>调用RocketMQTemplate的syncSend方法发送消息</li>
     *   <li><strong>记录发送结果：</strong>记录发送成功的日志，包含事件名称、发送状态、消息ID等</li>
     *   <li><strong>处理异常：</strong>捕获发送过程中的异常，记录失败日志后重新抛出</li>
     * </ol>
     * 
     * <p><strong>参数说明：</strong></p>
     * <ul>
     *   <li><strong>destination：</strong>目标地址，格式为 "Topic" 或 "Topic:Tag"</li>
     *   <li><strong>message：</strong>消息对象，由子类构建</li>
     *   <li><strong>timeout：</strong>发送超时时间，单位为毫秒</li>
     *   <li><strong>delayLevel：</strong>延迟级别，0表示不延迟，1-18表示不同的延迟时间</li>
     * </ul>
     */
    public SendResult sendMessage(T messageSendEvent) {
        // 构建消息发送扩展参数（由子类实现）
        BaseSendExtendDTO baseSendExtendDTO = buildBaseSendExtendParam(messageSendEvent);
        SendResult sendResult;
        try {
            // 构建目标地址：如果存在Tag，格式为 "Topic:Tag"，否则为 "Topic"
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());
            if (StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }
            
            // 同步发送消息到RocketMQ
            // 同步发送会阻塞直到收到Broker的响应，确保消息发送的可靠性
            sendResult = rocketMQTemplate.syncSend(
                    destinationBuilder.toString(),  // 目标地址（Topic或Topic:Tag）
                    buildMessage(messageSendEvent, baseSendExtendDTO),  // 消息对象（由子类实现）
                    baseSendExtendDTO.getSendTimeout(),  // 发送超时时间（毫秒）
                    Optional.ofNullable(baseSendExtendDTO.getDelayLevel()).orElse(0)  // 延迟级别（0表示不延迟）
            );
            
            // 记录发送成功日志：包含事件名称、发送状态、消息ID、业务标识
            // 便于后续问题排查和监控
            log.info("[{}] 消息发送结果：{}, 消息ID：{}, 消息Keys：{}", 
                    baseSendExtendDTO.getEventName(), 
                    sendResult.getSendStatus(), 
                    sendResult.getMsgId(), 
                    baseSendExtendDTO.getKeys());
        } catch (Throwable ex) {
            // 记录发送失败日志：包含事件名称、消息体内容、异常信息
            // 详细的日志有助于问题定位
            log.error("[{}] 消息发送失败，消息体：{}", 
                    baseSendExtendDTO.getEventName(), 
                    JSON.toJSONString(messageSendEvent), 
                    ex);
            // 重新抛出异常，让调用方处理
            // 确保调用方能够感知到消息发送失败
            throw ex;
        }
        return sendResult;
    }
}
