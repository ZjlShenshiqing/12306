/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.mq.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.biz.orderservice.common.enums.RefundTypeEnum;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;

import java.util.List;

/**
 * 退款结果回调订单服务事件
 *
 * @author zhangjlk
 * @date 2026/2/26 下午4:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class RefundResultCallBackOrderEvent {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款类型枚举
     */
    private RefundTypeEnum refundTypeEnum;

    /**
     * 部分退款车票详情
     */
    private List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList;
}
