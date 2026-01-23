package org.openzjl.index12306.biz.payservice.dto.base;

import java.math.BigDecimal;

/**
 * 支付入参接口
 *
 * @author zhangjlk
 * @date 2026/1/22 12:41
 */
public interface PayRequest {

    /**
     * 获取阿里支付入参
     */
    AliPayRequest getAliPayRequest();

    /**
     * 获取订单号
     */
    String getOrderSn();

    /**
     * 商户订单号
     */
    String getOrderRequestId();

    /**
     * 订单金额
     */
    BigDecimal getTotalAmount();

    /**
     * 构建查找支付策略实现类标识
     */
    String buildMark();
}
