/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.convert;

import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackCommand;
import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackRequest;
import org.openzjl.index12306.biz.payservice.dto.req.AliPayCallbackRequest;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;

import java.util.Objects;

/**
 * 支付回调请求入参转换器
 * <p>
 * 负责将通用的支付回调命令转换为具体支付渠道的回调请求对象，
 * 支持不同支付渠道的回调参数适配和转换。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/1/27 11:57
 */
public class PayCallbackRequestConvert {

    /**
     * 将支付回调命令转换为支付回调请求对象
     * <p>
     * 根据支付渠道类型，将通用的 PayCallbackCommand 转换为对应渠道的具体回调请求对象，
     * 确保不同支付渠道的回调参数能够正确适配到系统内部的处理流程。
     * </p>
     *
     * @param payCallbackCommand 支付回调命令
     *                           <ul>
     *                             <li>包含支付渠道、订单信息、回调数据等通用字段</li>
     *                             <li>作为转换的数据源，提供基础信息</li>
     *                           </ul>
     * @return 支付回调请求对象
     *         <ul>
     *           <li>根据支付渠道类型返回对应的具体实现类</li>
     *           <li>目前支持：AliPayCallbackRequest（支付宝回调请求）</li>
     *           <li>如果支付渠道不支持或参数无效，返回 null</li>
     *         </ul>
     * 
     * <p><strong>支持的支付渠道：</strong></p>
     * <ul>
     *   <li><strong>ALI_PAY：</strong>支付宝回调，转换为 AliPayCallbackRequest</li>
     *   <li><strong>其他渠道：</strong>待扩展...</li>
     * </ul>
     * 
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>初始化回调请求对象为 null</li>
     *   <li>判断支付渠道类型</li>
     *   <li>对于支付宝渠道：
     *     <ul>
     *       <li>使用 BeanUtil.convert 将命令对象转换为 AliPayCallbackRequest</li>
     *       <li>设置订单请求 ID（orderRequestId）</li>
     *     </ul>
     *   </li>
     *   <li>返回转换后的回调请求对象</li>
     * </ol>
     */
    public static PayCallbackRequest command2PayCallbackRequest(PayCallbackCommand payCallbackCommand) {
        // 初始化回调请求对象为 null
        PayCallbackRequest payCallbackRequest = null;
        
        // 判断支付渠道是否为支付宝
        if (Objects.equals(payCallbackCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            // 将通用命令对象转换为支付宝特定的回调请求对象
            payCallbackRequest = BeanUtil.convert(payCallbackCommand, AliPayCallbackRequest.class);
            // 设置订单请求 ID，确保回调与原始请求关联
            ((AliPayCallbackRequest) payCallbackRequest).setOrderRequestId(payCallbackCommand.getOrderRequestId());
        }
        
        // 返回转换后的回调请求对象
        // 如果支付渠道不支持，返回 null
        return payCallbackRequest;
    }
}
