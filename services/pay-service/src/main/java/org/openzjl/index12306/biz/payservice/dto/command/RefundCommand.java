/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.command;

import lombok.Data;
import org.openzjl.index12306.biz.payservice.dto.base.AbstractRefundRequest;

import java.math.BigDecimal;

/**
 * 退款请求命令
 *
 * @author zhangjlk
 * @date 2026/1/28 10:27
 */
@Data
public final class RefundCommand extends AbstractRefundRequest {

    /**
     * 支付金额
     */
    private BigDecimal payAmount;

    /**
     * 交易凭证号
     */
    private String tradeNo;
}
