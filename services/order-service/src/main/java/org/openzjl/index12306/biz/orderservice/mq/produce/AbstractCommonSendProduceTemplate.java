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
 * 提供统一的RocketMQ消息发送模板，封装了消息发送的通用逻辑
 * 
 * 使用模板方法模式：
 * 1. 子类需要实现 buildBaseSendExtendParam() 方法，构建消息发送的扩展参数
 * 2. 子类需要实现 buildMessage() 方法，构建具体的消息对象
 * 3. 调用 sendMessage() 方法即可完成消息发送
 * 
 * 功能特性：
 * - 支持Topic和Tag的组合（格式：Topic:Tag）
 * - 支持延迟消息发送
 * - 支持发送超时时间配置
 * - 完整的日志记录（成功和失败）
 * - 异常处理和重新抛出
 *
 * @param <T> 消息事件类型，由子类指定具体的消息事件类型
 * @author zhangjlk
 * @date 2026/1/20 11:57
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {

    /**
     * RocketMQ模板，用于发送消息
     */
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送的扩展参数
     * 子类需要实现此方法，根据消息事件构建BaseSendExtendDTO对象
     * BaseSendExtendDTO包含：事件名称、Topic、Tag、Keys、发送超时时间、延迟级别等
     *
     * @param messageEvent 消息事件对象
     * @return 消息发送扩展参数对象
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageEvent);

    /**
     * 构建消息对象
     * 子类需要实现此方法，根据消息事件和扩展参数构建Spring Message对象
     *
     * @param messageEvent 消息事件对象
     * @param requestParam 消息发送扩展参数
     * @return Spring Message对象
     */
    protected abstract Message<?> buildMessage(T messageEvent, BaseSendExtendDTO requestParam);

    /**
     * 发送消息到RocketMQ
     * 使用同步发送方式，等待发送结果返回
     * 
     * 发送流程：
     * 1. 构建消息发送扩展参数
     * 2. 构建目标地址（Topic或Topic:Tag）
     * 3. 构建消息对象
     * 4. 调用RocketMQ同步发送
     * 5. 记录发送结果日志
     * 6. 异常处理和重新抛出
     *
     * @param messageSendEvent 消息发送事件对象
     * @return RocketMQ发送结果，包含发送状态、消息ID等信息
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
            // 参数说明：
            // 1. destination：目标地址（Topic或Topic:Tag）
            // 2. message：消息对象（由子类实现）
            // 3. timeout：发送超时时间（毫秒）
            // 4. delayLevel：延迟级别（0表示不延迟，1-18表示不同的延迟时间）
            sendResult = rocketMQTemplate.syncSend(
                    destinationBuilder.toString(),
                    buildMessage(messageSendEvent, baseSendExtendDTO),
                    baseSendExtendDTO.getSendTimeout(),
                    Optional.ofNullable(baseSendExtendDTO.getDelayLevel()).orElse(0)
            );
            // 记录发送成功日志：事件名称、发送状态、消息ID、业务标识
            log.info("[{}] 消息发送结果：{}, 消息ID：{}, 消息Keys：{}", baseSendExtendDTO.getEventName(), sendResult.getSendStatus(), sendResult.getMsgId(), baseSendExtendDTO.getKeys());
        } catch (Throwable ex) {
            // 记录发送失败日志：事件名称、消息体内容、异常信息
            log.error("[{}] 消息发送失败，消息体：{}", baseSendExtendDTO.getEventName(), JSON.toJSONString(messageSendEvent), ex);
            // 重新抛出异常，让调用方处理
            throw ex;
        }
        return sendResult;
    }
}
