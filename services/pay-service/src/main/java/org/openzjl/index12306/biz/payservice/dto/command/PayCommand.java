/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.command;

import lombok.Data;
import org.openzjl.index12306.biz.payservice.dto.base.AbstractPayRequest;

import java.math.BigDecimal;

/**
 * 支付请求命令
 *
 * @author zhangjlk
 * @date 2026/1/22 12:38
 */
@Data
public final class PayCommand extends AbstractPayRequest {

    /**
     * 子订单号
     */
    private String orderSn;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单标题
     */
    private String subject;
}
