package org.openzjl.index12306.biz.orderservice.service;

import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;

/**
 *
 * @author zhangjlk
 * @date 2026/1/14 11:35
 */
public interface OrderService {

    /**
     * 根据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return        车票订单
     */
    TicketOrderDetailRespDTO queryTicketByOrderSn(String orderSn);
}
