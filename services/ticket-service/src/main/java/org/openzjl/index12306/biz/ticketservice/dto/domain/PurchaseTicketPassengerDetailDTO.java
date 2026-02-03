/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.dto.domain;

import lombok.Data;

/**
 * 购票乘车人详情实体
 *
 * @author zhangjlk
 * @date 2025/12/13 下午3:21
 */
@Data
public class PurchaseTicketPassengerDetailDTO {

    /**
     * 乘车人ID
     */
    private String passengerId;

    /**
     * 座位类型
     */
    private Integer seatType;
}
