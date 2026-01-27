package org.openzjl.index12306.biz.payservice.dto.base;

import org.openzjl.index12306.biz.payservice.dto.req.AliPayCallbackRequest;

/**
 * 支付回调请求入参
 *
 * @author zhangjlk
 * @date 2026/1/26 12:11
 */
public interface PayCallbackRequest {

    /**
     * 获取阿里支付回调入参
     */
    AliPayCallbackRequest getAliPayCallbackRequest();

    /**
     * 构建查找支付回调策略实现类标识
     */
    String buildMark();
}
