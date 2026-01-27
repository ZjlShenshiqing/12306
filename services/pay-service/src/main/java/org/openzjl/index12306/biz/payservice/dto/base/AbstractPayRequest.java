package org.openzjl.index12306.biz.payservice.dto.base;

import lombok.Getter;
import lombok.Setter;
import org.openzjl.index12306.biz.payservice.dto.req.AliPayRequest;
import org.zjl.index12306.framework.starter.distributedid.toolkit.SnowflakeIdUtil;

/**
 * 抽象支付入参实体
 *
 * @author zhangjlk
 * @date 2026/1/22 12:40
 */
public abstract class AbstractPayRequest implements PayRequest {

    /**
     * 交易环境
     * 网页/小程序/网站
     */
    @Getter
    @Setter
    private Integer tradeType;

    /**
     * 订单号
     */
    @Getter
    @Setter
    private String orderSn;

    /**
     * 支付渠道
     */
    @Getter
    @Setter
    private Integer channel;

    /**
     * 商户订单号
     * 由商家自定义，64个字符以内
     */
    @Getter
    @Setter
    private String orderRequestId = SnowflakeIdUtil.nextIdStr();

    @Override
    public AliPayRequest getAliPayRequest() {
        return null;
    }

    @Override
    public String getOrderRequestId() {
        return orderRequestId;
    }

    @Override
    public String buildMark() {
        return null;
    }
}
