/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.remote.dto;

import lombok.Data;
import org.openzjl.index12306.biz.ticketservice.common.enums.RefundTypeEnum;

import java.util.List;

/**
 * 退款请求入参实体
 *
 * @author zhangjlk
 * @date 2025/12/15 上午10:24
 */
@Data
public class RefundReqDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款类型枚举
     */
    private RefundTypeEnum refundTypeEnum;

    /**
     * 退款金额
     */
    private Integer refundAmount;

    /**
     * 部分车票退款详情集合
     */
    private List<TicketOrderPassengerDetailRespDTO> refundDetailReqDTOList;
}
