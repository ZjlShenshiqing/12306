package org.openzjl.index12306.biz.payservice.service;

import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.biz.payservice.dto.resp.PayInfoRespDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.PayRespDTO;

/**
 * 支付接口层
 *
 * @author zhangjlk
 * @date 2026/1/22 12:30
 */
public interface PayService {

    /**
     * 创建支付单
     *
     * @param payRequest 创建支付单实体
     * @return 支付返回详情
     */
    PayRespDTO commonPay(PayRequest payRequest);

    /**
     * 根据订单号查询支付单详情
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfoByOrderSn(String orderSn);

    /**
     * 根据支付流水号查询支付单详情
     *
     * @param paySn 支付流水号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfoByPaySn(String paySn);
}
