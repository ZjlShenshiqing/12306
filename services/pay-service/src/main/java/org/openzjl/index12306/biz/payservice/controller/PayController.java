package org.openzjl.index12306.biz.payservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.payservice.convert.PayRequestConvert;
import org.openzjl.index12306.biz.payservice.dto.PayCommand;
import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.biz.payservice.dto.resp.PayRespDTO;
import org.openzjl.index12306.biz.payservice.service.PayService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付控制层
 * <p>
 * 对外提供统一的支付入口：接收上游支付指令 {@link PayCommand}，并将其转换为具体渠道的
 * {@link PayRequest} 请求对象，最后交由 {@link PayService} 执行业务支付逻辑。
 * <p>
 * 说明：
 * <ul>
 *   <li>渠道适配：通过 {@link PayRequestConvert} 将统一入参转换为不同支付渠道的请求对象</li>
 *   <li>结果包装：统一使用 {@link Result} + {@link Results} 返回标准响应</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2026/1/22 12:29
 */
@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /**
     * 公共支付接口
     *
     * 对接常用支付方式（如支付宝、微信、银行卡等）的统一入口。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>接收统一支付指令 {@link PayCommand}</li>
     *   <li>按渠道转换为具体 {@link PayRequest}（当前转换逻辑在 {@link PayRequestConvert}）</li>
     *   <li>调用 {@link PayService#commonPay(PayRequest)} 执行支付</li>
     *   <li>返回统一的 {@link Result} 响应</li>
     * </ol>
     *
     * @param requestParam 支付请求指令（包含渠道、交易类型、订单号、金额等）
     * @return 支付结果（如支付跳转信息、二维码链接、预支付信息等，取决于具体渠道/交易类型）
     */
    public Result<PayRespDTO> pay(@RequestBody PayCommand requestParam) {
        PayRequest payRequest = PayRequestConvert.command2PayRequest(requestParam);
        PayRespDTO result = payService.commonPay(payRequest);
        return Results.success(result);
    }
}
