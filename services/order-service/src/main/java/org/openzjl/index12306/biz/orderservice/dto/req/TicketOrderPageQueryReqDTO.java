/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.dto.req;

import lombok.Data;
import org.openzjl.index12306.framework.starter.convention.page.PageRequest;

/**
 * 车票订单分页查询
 *
 * @author zhangjlk
 * @date 2026/1/15 15:09
 */
@Data
public class TicketOrderPageQueryReqDTO extends PageRequest {

    /**
     * 用户唯一标识
     */
    private String userId;

    /**
     * 状态类型
     *
     * 0：未完成
     * 1：未出行
     * 2：历史订单
     */
    private Integer statusType;
}
