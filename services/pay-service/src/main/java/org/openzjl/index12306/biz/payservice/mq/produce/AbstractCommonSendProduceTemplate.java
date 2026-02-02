package org.openzjl.index12306.biz.payservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

/**
 * 抽象公共发送信息组件
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
 * <p><strong>功能特性：</strong></p>
 * <ul>
 *   <li><strong>支持Topic和Tag的组合：</strong>自动构建目标地址，格式为 "Topic:Tag"</li>
 *   <li><strong>支持发送超时配置：</strong>可设置消息发送的超时时间，避免无限等待</li>
 *   <li><strong>完整的日志记录：</strong>记录发送结果和失败原因，便于问题排查</li>
 *   <li><strong>统一的异常处理：</strong>捕获并记录异常，然后重新抛出，确保调用方感知</li>
 * </ul>
 *
 * @param <T> 消息事件类型，由子类指定具体的消息事件类型
 * @author zhangjlk
 * @date 2026/1/30 15:57
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
     * 构建消息发送事件基础扩充属性实体
     * <p>
     * 子类必须实现此方法，根据具体的消息事件构建消息发送的扩展参数，
     * 包括事件名称、Topic、Tag、Keys、发送超时时间等信息。
     * </p>
     *
     * @param messageEvent 消息发送事件
     * @return 扩充属性实体
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageEvent);

    /**
     * 构建消息基本参数，请求头、Keys
     * <p>
     * 子类必须实现此方法，根据消息事件和扩展参数构建Spring Message对象，
     * 负责将业务数据转换为RocketMQ可发送的消息格式，
     * 可设置消息头、Keys等信息。
     * </p>
     *
     * @param messageSendEvent 消息发送事件
     * @param requestParam 扩充属性实体
     * @return 消息基本参数
     */
    protected abstract Message<?> buildMessage(T messageSendEvent, BaseSendExtendDTO requestParam);

    /**
     * 消息事件通用发送
     * <p>
     * 使用同步发送方式，等待发送结果返回，确保消息发送的可靠性。
     * 此方法是模板类的核心方法，定义了消息发送的完整流程。
     * </p>
     *
     * @param messageSendEvent 消息发送事件
     * @return 消息发送返回结果
     * @throws Throwable 发送失败时抛出异常
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
                    baseSendExtendDTO.getSentTimeout()  // 发送超时时间（毫秒）
            );
            
            // 记录发送成功日志：包含事件名称、发送状态、消息ID、业务标识
            // 便于后续问题排查和监控
            log.info("[{}] 消息发送结果：{}，消息ID：{}，消息Keys：{}", 
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
