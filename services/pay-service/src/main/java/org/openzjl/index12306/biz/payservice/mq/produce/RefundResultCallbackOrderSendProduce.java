package org.openzjl.index12306.biz.payservice.mq.produce;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.openzjl.index12306.biz.payservice.mq.domain.MessageWrapper;
import org.openzjl.index12306.biz.payservice.mq.event.RefundResultCallBackOrderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.openzjl.index12306.biz.payservice.common.constant.PayRocketMQConstant.PAY_GLOBAL_TOPIC_KEY;
import static org.openzjl.index12306.biz.payservice.common.constant.PayRocketMQConstant.REFUND_RESULT_CALLBACK_TAG_KEY;

/**
 * 退款结果回调给订单服务的消息生产者
 * <p>
 * 继承自 AbstractCommonSendProduceTemplate，实现了退款结果回调消息的发送逻辑，
 * 负责将退款结果通过 RocketMQ 发送给订单服务，以便订单服务更新订单状态。
 * </p>
 * 
 * <p><strong>功能说明：</strong></p>
 * <ul>
 *   <li>构建退款结果回调消息的发送参数</li>
 *   <li>封装退款结果回调消息</li>
 *   <li>发送消息到 RocketMQ，通知订单服务处理退款结果</li>
 * </ul>
 * 
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>当用户发起退款申请并处理完成后，通知订单服务更新订单状态</li>
 *   <li>支持全部退款和部分退款的结果回调</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2026/1/30 15:53
 */
@Slf4j
@Component
public class RefundResultCallbackOrderSendProduce extends AbstractCommonSendProduceTemplate<RefundResultCallBackOrderEvent> {

    /**
     * 配置环境
     * <p>
     * 用于获取应用配置，如 RocketMQ 的 Topic 和 Tag 配置，
     * 通过 environment.resolvePlaceholders() 方法解析配置占位符。
     * </p>
     */
    private final ConfigurableEnvironment environment;

    /**
     * 构造函数
     * <p>
     * 初始化退款结果回调消息生产者，设置 RocketMQTemplate 和 ConfigurableEnvironment。
     * </p>
     *
     * @param rocketMQTemplate RocketMQ 模板
     *                         <ul>
     *                           <li>用于发送消息到 RocketMQ</li>
     *                           <li>传递给父类构造函数</li>
     *                         </ul>
     * @param environment 配置环境
     *                    <ul>
     *                      <li>用于获取 RocketMQ 的 Topic 和 Tag 配置</li>
     *                      <li>存储为成员变量，供后续方法使用</li>
     *                    </ul>
     */
    public RefundResultCallbackOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        // 调用父类构造函数，传入 RocketMQTemplate
        super(rocketMQTemplate);
        // 初始化配置环境成员变量
        this.environment = environment;
    }

    /**
     * 构建消息发送事件基础扩充属性实体
     * <p>
     * 根据退款结果回调订单事件，构建消息发送的扩展参数，
     * 包括事件名称、业务标识、Topic、Tag 和发送超时时间。
     * </p>
     *
     * @param messageEvent 退款结果回调订单事件
     * @return 扩充属性实体
     */
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(RefundResultCallBackOrderEvent messageEvent) {
        // 使用建造者模式构建 BaseSendExtendDTO
        return BaseSendExtendDTO.builder()
                // 设置事件名称
                .eventName("全部退款或部分退款结果回调订单")
                // 设置业务标识为订单号
                .keys(messageEvent.getOrderSn())
                // 从配置中解析消息主题
                .topic(environment.resolvePlaceholders(PAY_GLOBAL_TOPIC_KEY))
                // 从配置中解析消息标签
                .tag(environment.resolvePlaceholders(REFUND_RESULT_CALLBACK_TAG_KEY))
                // 设置发送超时时间为 2000 毫秒
                .sentTimeout(2000L)
                // 构建并返回
                .build();
    }

    /**
     * 构建消息基本参数，请求头、Keys
     * <p>
     * 根据退款结果回调订单事件和扩展参数，构建 Spring Message 对象，
     * 封装消息体和消息头，设置消息的 Keys 和 Tags。
     * </p>
     *
     * @param messageSendEvent 退款结果回调订单事件
     * @param requestParam 扩充属性实体
     * @return 消息对象
     */
    @Override
    protected Message<?> buildMessage(RefundResultCallBackOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        // 生成消息的 Keys：如果扩展参数中的 Keys 为空，则生成 UUID，否则使用扩展参数中的 Keys
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        
        // 使用 MessageBuilder 构建消息
        return MessageBuilder
                // 设置消息体：使用 MessageWrapper 包装扩展参数中的 Keys 和事件对象
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                // 设置消息头：消息的 Keys，用于消息查询和追踪
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                // 设置消息头：消息的 Tags，用于消息过滤
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                // 构建并返回 Message 对象
                .build();
    }
}
