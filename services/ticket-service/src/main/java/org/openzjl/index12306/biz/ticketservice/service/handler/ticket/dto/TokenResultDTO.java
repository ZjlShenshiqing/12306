/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 令牌扣减返回参数
 *
 * @author zhangjlk
 * @date 2025/12/16 上午9:35
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResultDTO {

    /**
     * Token是否为空
     */
    private Boolean tokenIsNull;

    /**
     * 获取 Token 为空站点座位类型和数量
     */
    private List<String> tokenIsNullSeatTypeCounts;
}
