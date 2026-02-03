/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车票购买返回参数
 *
 * @author zhangjlk
 * @date 2025/12/5 上午9:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketPurchaseRespDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 乘车人订单详情列表
     */
    private List<TicketOrderDetailRespDTO> ticketOrderDetails;
}
