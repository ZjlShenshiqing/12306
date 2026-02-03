/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.mq.produce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发送事件基础扩充属性实体
 *
 * @author zhangjlk
 * @date 2026/1/20 11:59
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseSendExtendDTO {

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    /**
     * 业务标识
     */
    private String keys;

    /**
     * 发送超时时间
     */
    private Long sendTimeout;

    /**
     * 延迟消息
     */
    private Integer delayLevel;
}
