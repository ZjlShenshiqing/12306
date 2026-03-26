/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.service;

import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;

/**
 * 订单查询兜底服务：当 ShardingSphere 路由查不到时，直接遍历物理表
 *
 * @author zhangjlk
 */
public interface OrderQueryFallbackService {

    /**
     * 按订单号从物理表兜底查询
     *
     * @param orderSn 订单号
     * @return 订单详情，查不到返回 null
     */
    TicketOrderDetailRespDTO queryByOrderSnFallback(String orderSn);
}
