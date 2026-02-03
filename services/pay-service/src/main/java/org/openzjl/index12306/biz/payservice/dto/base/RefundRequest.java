/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.base;

import org.openzjl.index12306.biz.payservice.dto.req.AliRefundRequest;

/**
 * 退款入参接口
 *
 * @author zhangjlk
 * @date 2026/1/28 10:31
 */
public interface RefundRequest {

    /**
     * 获取阿里退款入参
     */
    AliRefundRequest getAliRefundRequest();

    /**
     * 获取订单号
     */
    String getOrderSn();

    /**
     * 构建查找支付策略实现类标识
     */
    String buildMark();
}
