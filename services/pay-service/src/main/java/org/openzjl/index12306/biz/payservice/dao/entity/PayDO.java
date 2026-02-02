package org.openzjl.index12306.biz.payservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.openzjl.index12306.framework.starter.database.base.BaseDO;

/**
 * 支付实体
 *
 * @author zhangjlk
 * @date 2026/1/23 16:27
 */
@Data
@TableName("t_pay")
public class PayDO extends BaseDO {
    
    /**
     * ID
     */
    private Long id;

    /**
     * 支付流水号
     */
    private String paySn;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 商户订单号
     */
    private String outOrderSn;

    /**
     * 支付渠道
     */
    private Integer channel;

    /**
     * 交易类型
     */
    private Integer tradeType;

    /**
     * 订单标题
     */
    private String subject;

    /**
     * 交易凭证号
     */
    private String tradeNo;

    /**
     * 商户订单号
     */
    private String orderRequestId;

    /**
     * 订单总金额
     */
    private Integer totalAmount;

    /**
     * 实际支付金额
     */
    private Integer payAmount;

    /**
     * 支付状态
     */
    private String status;
}
