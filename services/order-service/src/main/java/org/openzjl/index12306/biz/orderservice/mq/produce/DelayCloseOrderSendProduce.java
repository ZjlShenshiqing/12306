package org.openzjl.index12306.biz.orderservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.openzjl.index12306.biz.orderservice.common.constant.OrderRocketMQConstant;
import org.openzjl.index12306.biz.orderservice.mq.domain.MessageWrapper;
import org.openzjl.index12306.biz.orderservice.mq.event.DelayCloseOrderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 延迟关闭订单消息生产者
 * 
 * 业务场景：
 * 当用户创建订单但未在规定时间内支付时，系统需要自动关闭订单并释放车票库存
 * 本生产者负责发送延迟消息，在订单创建后延迟一定时间（延迟级别14，约10分钟）后触发订单关闭流程
 * 
 * 工作流程：
 * 1. 订单创建后，调用本生产者发送延迟消息
 * 2. 消息在RocketMQ中延迟指定时间后投递
 * 3. 消费者接收到消息后执行订单关闭逻辑
 * 
 * 延迟级别说明：
 * - delayLevel=14 对应延迟时间约10分钟
 * - RocketMQ延迟级别：1-18分别对应不同的延迟时间（1s, 5s, 10s, 30s, 1m, 2m, 3m, 4m, 5m, 6m, 7m, 8m, 9m, 10m, 20m, 30m, 1h, 2h）
 *
 * @author zhangjlk
 * @date 2026/1/20 11:53
 */
@Slf4j
@Component
public class DelayCloseOrderSendProduce extends AbstractCommonSendProduceTemplate<DelayCloseOrderEvent>{

    /**
     * Spring环境配置，用于解析配置文件中的占位符
     * 例如：解析 ${unique-name:} 这样的占位符
     */
    private final ConfigurableEnvironment environment;

    /**
     * 构造函数
     *
     * @param rocketMQTemplate RocketMQ模板，用于发送消息
     * @param environment Spring环境配置
     */
    public DelayCloseOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    /**
     * 构建消息发送的扩展参数
     * 配置消息的Topic、Tag、延迟级别、超时时间等参数
     *
     * @param messageEvent 延迟关闭订单事件，包含订单号、车次信息等
     * @return 消息发送扩展参数对象
     */
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(DelayCloseOrderEvent messageEvent) {
        return BaseSendExtendDTO.builder()
                // 事件名称，用于日志记录
                .eventName("延迟关闭订单")
                // 业务标识（Keys），使用订单号作为唯一标识，便于消息追踪和幂等性处理
                .keys(messageEvent.getOrderSn())
                // Topic：从配置文件中解析，支持占位符替换（如 ${unique-name:}）
                .topic(environment.resolvePlaceholders(OrderRocketMQConstant.ORDER_DELAY_CLOSE_TOPIC_KEY))
                // Tag：从配置文件中解析，用于消息过滤
                .tag(environment.resolvePlaceholders(OrderRocketMQConstant.ORDER_DELAY_CLOSE_TAG_KEY))
                // 发送超时时间：2秒（2000毫秒）
                .sendTimeout(2000L)
                // 延迟级别：14，对应延迟时间约10分钟
                // 订单创建后10分钟，如果用户未支付，则触发订单关闭
                .delayLevel(14)
                .build();
    }

    /**
     * 构建消息对象
     * 将事件对象包装成Spring Message对象，并设置消息头信息
     *
     * @param messageEvent 延迟关闭订单事件
     * @param requestParam 消息发送扩展参数
     * @return Spring Message对象
     */
    @Override
    protected Message<?> buildMessage(DelayCloseOrderEvent messageEvent, BaseSendExtendDTO requestParam) {
        // 如果Keys为空，则生成UUID作为备用Keys（保证消息可追踪）
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        // 构建消息对象
        return MessageBuilder
                // 消息体：使用MessageWrapper包装订单号和事件对象
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageEvent))
                // 设置消息Keys头：用于消息查询和幂等性处理
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                // 设置消息Tag头：用于消息过滤
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
