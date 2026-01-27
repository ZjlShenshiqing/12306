package org.openzjl.index12306.biz.payservice.dto.base;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付回调请求命令
 *
 * @author zhangjlk
 * @date 2026/1/26 10:45
 */
@Data
public class PayCallbackCommand extends AbstractPayRequest{

    /**
     * 支付渠道
     */
    private Integer channel;

    /**
     * 支付状态
     */
    @JsonAlias("trade_status")
    private String tradeStatus;

    /**
     * 支付凭证号
     */
    @JsonAlias("trade_no")
    private String tradeNo;

    /**
     * 买家付款时间
     */
    @JsonAlias("gmt_payment")
    private Date gmtPayment;

    /**
     * 买家付款金额
     */
    @JsonAlias("buyer_pay_amount")
    private BigDecimal buyerPayAmount;

    /**
     * 商户订单号
     */
    @JsonAlias("out_trade_no")
    private String outTradeNo;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单标题
     */
    private String subject;
}
