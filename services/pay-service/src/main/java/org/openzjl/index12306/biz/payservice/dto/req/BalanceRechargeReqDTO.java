/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.req;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 余额充值请求参数
 */
@Data
public class BalanceRechargeReqDTO {

    /**
     * 充值金额（元）
     */
    private BigDecimal amount;
}
