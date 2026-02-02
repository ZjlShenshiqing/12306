package org.openzjl.index12306.biz.payservice.convert;

import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.dto.command.PayCommand;
import org.openzjl.index12306.biz.payservice.dto.req.AliPayRequest;
import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;

import java.util.Objects;

/**
 * 支付请求入参转换器
 * <p>
 * 将上游的支付指令 {@link PayCommand} 转换为具体渠道的请求对象（如支付宝、微信等）。
 *
 * @author zhangjlk
 * @date 2026/1/23 15:43
 */
public final class PayRequestConvert {

    /**
     * 将支付指令转换为具体支付渠道的请求对象
     *
     * @param command 统一的支付指令（包含渠道、金额、订单号等信息）
     * @return 渠道对应的 {@link PayRequest} 实现；当前仅支持 {@link AliPayRequest}，
     * 若渠道不支持则返回 {@code null}
     */
    public static PayRequest command2PayRequest(PayCommand command) {
        PayRequest payRequest = null;
        if (Objects.equals(command.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            payRequest = BeanUtil.convert(command, AliPayRequest.class);
        }
        return payRequest;
    }
}
