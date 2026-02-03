/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.req;

import lombok.Data;
import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.dto.base.AbstractPayCallbackRequest;
import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackRequest;

/**
 * 支付宝回调请求入参
 *
 * @author zhangjlk
 * @date 2026/1/26 12:12
 */
@Data
public class AliPayCallbackRequest extends AbstractPayCallbackRequest {

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 支付状态
     */
    private String tradeStatus;

    /**
     * 支付凭证号
     */
    private String tradeNo;

    /**
     * 买家付款时间
     */
    private String gmtPayment;

    /**
     * 买家付款金额
     */
    private Integer buyerPayAmount;

    @Override
    public AliPayCallbackRequest getAliPayCallbackRequest() {
        return this;
    }

    @Override
    public String buildMark() {
        return PayChannelEnum.ALI_PAY.getName();
    }
}
