/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.base;

import lombok.Getter;
import lombok.Setter;
import org.openzjl.index12306.biz.payservice.dto.req.AliRefundRequest;

/**
 * 抽象退款入参实体
 *
 * @author zhangjlk
 * @date 2026/1/28 10:31
 */
public abstract class AbstractRefundRequest implements RefundRequest {

    /**
     * 交易环境
     * H5、小程序、网站等
     */
    @Getter
    @Setter
    private Integer tradeType;

    /**
     * 订单号
     */
    @Getter
    @Setter
    private String orderSn;

    /**
     * 支付渠道
     */
    @Getter
    @Setter
    private Integer channel;

    @Override
    public AliRefundRequest getAliRefundRequest() {
        return null;
    }

    @Override
    public String buildMark() {
        return null;
    }
}
