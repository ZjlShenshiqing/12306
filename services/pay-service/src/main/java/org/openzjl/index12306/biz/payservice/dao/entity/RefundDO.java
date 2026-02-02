package org.openzjl.index12306.biz.payservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 退款记录实体
 *
 * @author zhangjlk
 * @date 2026/1/28 10:01
 */
@Data
@TableName("t_refund")
public class RefundDO {
    
    /**
     * 退款记录ID
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
     * 交易流水号
     */
    private String tradeNo;

    /**
     * 退款金额
     */
    private Integer amount;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 列车ID
     */
    private Long trainId;

    /**
     * 列车车次号
     */
    private String trainNumber;

    /**
     * 乘车日期
     */
    private Date ridingDate;

    /**
     * 出发地
     */
    private String departure;

    /**
     * 到达地
     */ 
    private String arrival;

    /**
     * 出发时间
     */
    private Date departureTime;

    /**
     * 到达时间
     */ 
    private Date arrivalTime;

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 退款时间
     */
    private Date refundTime;
}
