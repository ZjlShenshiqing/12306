/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.mq.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付结果回调订单服务事件
 *
 * @author zhangjlk
 * @date 2026/1/21 19:58
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayResultCallbackOrderEvent {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 商户订单号
     */
    private String outOrderSn;

    /**
     * 支付渠道
     */
    private Integer channel;

    /**
     * 交易类型
     */
    private String tradeType;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 交易凭证号
     */
    private String tradeNo;

    /**
     * 商户订单号
     */
    private String orderRequestId;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 支付时间
     */
    private Date gmtPayment;

    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     * 支付状态
     */
    private String status;
}
