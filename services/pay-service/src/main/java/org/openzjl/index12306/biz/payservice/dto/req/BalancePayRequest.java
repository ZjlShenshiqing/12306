/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.req;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.dto.base.AbstractPayRequest;

import java.math.BigDecimal;

/**
 * 余额支付请求入参
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public final class BalancePayRequest extends AbstractPayRequest {

    /**
     * 商户订单号
     */
    private String outOrderSn;

    /**
     * 订单金额（元）
     */
    private BigDecimal totalAmount;

    /**
     * 订单标题
     */
    private String subject;

    @Override
    public String buildMark() {
        return PayChannelEnum.BALANCE_PAY.getName();
    }
}
