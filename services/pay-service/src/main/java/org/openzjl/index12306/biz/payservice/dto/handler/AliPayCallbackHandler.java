/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.common.enums.TradeStatusEnum;
import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackRequest;
import org.openzjl.index12306.biz.payservice.dto.handler.base.AbstractPayCallbackHandler;
import org.openzjl.index12306.biz.payservice.dto.req.AliPayCallbackRequest;
import org.openzjl.index12306.biz.payservice.dto.req.PayCallbackReqDTO;
import org.openzjl.index12306.biz.payservice.service.PayService;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractExecuteStrategy;
import org.springframework.stereotype.Service;

/**
 * 阿里支付回调组件
 *
 * @author zhangjlk
 * @date 2026/2/28 下午12:08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class AliPayCallbackHandler extends AbstractPayCallbackHandler implements AbstractExecuteStrategy<PayCallbackRequest, Void> {

    private final PayService payService;

    @Override
    public void callback(PayCallbackRequest payCallbackRequest) {
        AliPayCallbackRequest aliPayCallBackRequest = payCallbackRequest.getAliPayCallbackRequest();
        PayCallbackReqDTO payCallbackRequestParam = PayCallbackReqDTO.builder()
                .status(TradeStatusEnum.queryActualTradeStatusCode(aliPayCallBackRequest.getTradeStatus()))
                .payAmount(aliPayCallBackRequest.getBuyerPayAmount())
                .tradeNo(aliPayCallBackRequest.getTradeNo())
                .gmtPayment(aliPayCallBackRequest.getGmtPayment())
                .orderSn(aliPayCallBackRequest.getOrderRequestId())
                .build();
        payService.callbackPay(payCallbackRequestParam);
    }

    @Override
    public String mark() {
        return PayChannelEnum.ALI_PAY.name();
    }

    public void execute(PayCallbackRequest requestParam) {
        callback(requestParam);
    }
}
