/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.dto.req;

import lombok.Data;

/**
 * 取消车票订单请求入参
 *
 * @author zhangjlk
 * @date 2026/1/21 11:27
 */
@Data
public class CancelTicketOrderReqDTO {

    /**
     * 订单号
     */
    private String orderSn;
}
