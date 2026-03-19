/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.convert;

import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.dto.command.PayCommand;
import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.biz.payservice.dto.req.AliPayRequest;
import org.openzjl.index12306.biz.payservice.dto.req.BalancePayRequest;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;

import java.util.Objects;

/**
 * 支付请求入参转换器
 *
 * @author zhangjlk
 * @date 2026/1/23 15:43
 */
public final class PayRequestConvert {

    /**
     * 将支付指令转换为具体支付渠道的请求对象
     */
    public static PayRequest command2PayRequest(PayCommand command) {
        PayRequest payRequest = null;
        if (Objects.equals(command.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            payRequest = BeanUtil.convert(command, AliPayRequest.class);
        } else if (Objects.equals(command.getChannel(), PayChannelEnum.BALANCE_PAY.getCode())) {
            payRequest = BeanUtil.convert(command, BalancePayRequest.class);
        }
        return payRequest;
    }
}
