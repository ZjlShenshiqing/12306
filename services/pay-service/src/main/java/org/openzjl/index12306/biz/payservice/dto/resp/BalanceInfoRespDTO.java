/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户余额信息返回参数
 */
@Data
@Builder
public class BalanceInfoRespDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 余额（元）
     */
    private BigDecimal balance;
}
