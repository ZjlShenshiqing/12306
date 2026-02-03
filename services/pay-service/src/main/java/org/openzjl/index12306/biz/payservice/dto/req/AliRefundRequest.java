/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.req;

import lombok.Data;
import lombok.experimental.Accessors;
import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.common.enums.PayTradeTypeEnum;
import org.openzjl.index12306.biz.payservice.common.enums.TradeStatusEnum;
import org.openzjl.index12306.biz.payservice.dto.base.AbstractRefundRequest;

import java.math.BigDecimal;

/**
 * 支付宝退款请求入参
 * <p>
 * 继承自 AbstractRefundRequest，用于封装支付宝退款相关的请求参数，
 * 包含支付金额、交易号等支付宝特有的退款信息。
 * 同时实现了策略模式所需的标记生成方法，用于选择对应的退款处理策略。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/1/28 10:33
 */
@Data
@Accessors(chain = true)
public class AliRefundRequest extends AbstractRefundRequest {

    /**
     * 支付金额
     * <p>
     * 原始支付的总金额，用于计算退款后的剩余金额。
     * 类型为 BigDecimal，保持金额计算的精度。
     * </p>
     */
    private BigDecimal payAmount;

    /**
     * 支付宝交易号
     * <p>
     * 支付宝生成的唯一交易凭证号，用于标识支付宝交易。
     * 在退款时，支付宝需要通过此交易号找到对应的原始交易。
     * </p>
     */
    private String tradeNo;

    /**
     * 获取当前对象实例
     * <p>
     * 重写父类方法，返回当前 AliRefundRequest 实例，
     * 用于类型转换和链式调用。
     * </p>
     *
     * @return 当前 AliRefundRequest 实例
     *         <ul>
     *           <li>返回 this 指针，便于链式调用和类型确定</li>
     *         </ul>
     */
    @Override
    public AliRefundRequest getAliRefundRequest() {
        return this;
    }

    /**
     * 生成策略选择标记
     * <p>
     * 重写父类方法，根据支付渠道、交易类型和交易状态生成策略选择标记，
     * 用于策略模式中选择对应的支付宝退款处理策略。
     * </p>
     *
     * @return 策略选择标记
     *         <ul>
     *           <li>格式：ALI_PAY[_交易类型名称][交易关闭状态码]</li>
     *           <li>示例1：ALI_PAY（无交易类型）</li>
     *           <li>示例2：ALI_PAY_APP0（APP交易，交易关闭状态码为0）</li>
     *         </ul>
     * 
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>初始化标记为支付渠道名称：ALI_PAY</li>
     *   <li>如果交易类型不为 null：
     *     <ul>
     *       <li>获取交易类型对应的名称（如 APP、WEB 等）</li>
     *       <li>获取交易关闭状态码（TRADE_CLOSED.tradeCode()）</li>
     *       <li>拼接格式：ALI_PAY_交易类型名称交易关闭状态码</li>
     *     </ul>
     *   </li>
     *   <li>返回生成的标记</li>
     * </ol>
     * 
     * <p><strong>标记用途：</strong></p>
     * <ul>
     *   <li>在策略模式中，用于选择对应的支付宝退款处理策略</li>
     *   <li>不同的交易类型可能需要不同的退款处理逻辑</li>
     *   <li>交易状态用于标识退款场景（如交易关闭时的退款）</li>
     * </ul>
     */
    @Override
    public String buildMark() {
        // 初始化标记为支付渠道名称：ALI_PAY
        String mark = PayChannelEnum.ALI_PAY.name();
        
        // 如果交易类型不为 null，拼接交易类型和交易状态
        if (getTradeType() != null) {
            // 获取交易类型对应的名称（如 APP、WEB 等）
            String tradeTypeName = PayTradeTypeEnum.findNameByCode(getTradeType());
            // 获取交易关闭状态码
            Integer closedStatusCode = TradeStatusEnum.TRADE_CLOSED.tradeCode();
            // 拼接格式：ALI_PAY_交易类型名称交易关闭状态码
            mark = PayChannelEnum.ALI_PAY.name() + "_" + tradeTypeName + closedStatusCode;
        }
        
        // 返回生成的策略选择标记
        return mark;
    }
}
