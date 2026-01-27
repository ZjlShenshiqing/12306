package org.openzjl.index12306.biz.payservice.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.convert.PayCallbackRequestConvert;
import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackCommand;
import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackRequest;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractStrategyChoose;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付结果回调控制器
 * <p>
 * 负责接收和处理第三方支付平台的回调通知，包括支付宝、微信支付等渠道的支付结果通知。
 * 采用策略模式处理不同支付渠道的回调逻辑，确保系统的可扩展性和可维护性。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/1/26 10:43
 */
@RestController
@RequiredArgsConstructor
public class PayCallbackController {

    /**
     * 策略选择器
     * <p>
     * 根据支付回调请求的标记选择对应的处理策略，
     * 支持不同支付渠道回调的统一处理。
     * </p>
     */
    private final AbstractStrategyChoose strategyChoose;

    /**
     * 支付宝回调接口
     * <p>
     * 接收支付宝支付结果通知，解析回调参数，转换为系统内部的回调命令，
     * 并通过策略模式分发到对应的处理逻辑。
     * </p>
     * <p>
     * <strong>接口说明：</strong>
     * <ul>
     *   <li>请求方式：POST</li>
     *   <li>请求路径：/api/pay-service/callback/alipay</li>
     *   <li>请求参数：支付宝回调通知的参数集合（Map形式）</li>
     *   <li>返回值：无（void）</li>
     * </ul>
     * </p>
     * <p>
     * <strong>回调参数说明：</strong>
     * <ul>
     *   <li>out_trade_no：商户订单号</li>
     *   <li>trade_no：支付宝交易号</li>
     *   <li>trade_status：交易状态</li>
     *   <li>total_amount：交易金额</li>
     *   <li>gmt_payment：支付时间</li>
     *   <li>其他支付宝回调参数...</li>
     * </ul>
     * </p>
     *
     * @param requestParam 支付宝回调通知的参数集合
     *                    包含支付宝返回的所有回调参数，以键值对形式存储
     * @see PayCallbackCommand
     * @see PayCallbackRequestConvert
     * @see AbstractStrategyChoose
     */
    @PostMapping("/api/pay-service/callback/alipay")
    public void callbackAlipay(@RequestParam Map<String, Object> requestParam) {
        // 将回调参数转换为支付回调命令对象
        // 使用 Hutool 的 BeanUtil.mapToBean 方法进行转换
        PayCallbackCommand payCallbackCommand = BeanUtil.mapToBean(requestParam, PayCallbackCommand.class, true, CopyOptions.create());
        
        // 设置支付渠道为支付宝
        payCallbackCommand.setChannel(PayChannelEnum.ALI_PAY.getCode());
        
        // 设置订单请求ID（使用支付宝回调中的商户订单号）
        payCallbackCommand.setOrderRequestId(requestParam.get("out_trade_no").toString());
        
        // 设置支付时间（解析支付宝回调中的支付时间）
        payCallbackCommand.setGmtPayment(DateUtil.parse(requestParam.get("gmt_payment").toString()));
        
        // 将支付回调命令转换为支付回调请求对象
        // 根据支付渠道类型返回对应的具体实现类
        PayCallbackRequest payCallbackRequest = PayCallbackRequestConvert.command2PayCallbackRequest(payCallbackCommand);
        
        // 使用策略模式处理回调请求
        // payCallbackRequest.buildMark() 生成策略选择标记
        // payCallbackRequest 作为处理参数
        strategyChoose.chooseAndExecute(payCallbackRequest.buildMark(), payCallbackRequest);
    }
}
