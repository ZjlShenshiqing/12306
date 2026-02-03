/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dto.domain.OrderItemStatusReversalDTO;
import org.openzjl.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;

import java.util.List;

/**
 *
 * @author zhangjlk
 * @date 2026/1/14 11:35
 */
public interface OrderItemService extends IService<OrderItemDO> {

    /**
     * 根据子订单记录id查询车票子订单详情
     *
     * @param requestParam 请求参数
     * @return 车票子订单详情
     */
    List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam);

    /**
     * 子订单（订单明细）状态反转
     *
     * @param requestParam
     */
    void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam);
}
