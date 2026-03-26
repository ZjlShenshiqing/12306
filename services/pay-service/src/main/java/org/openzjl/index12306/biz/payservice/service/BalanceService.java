/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.service;

import org.openzjl.index12306.biz.payservice.dto.resp.BalanceInfoRespDTO;

import java.math.BigDecimal;

/**
 * 用户余额服务
 */
public interface BalanceService {

    /**
     * 查询当前登录用户余额
     */
    BalanceInfoRespDTO queryCurrentUserBalance();

    /**
     * 当前登录用户充值
     *
     * @param amount 充值金额（元）
     * @return 充值后余额
     */
    BalanceInfoRespDTO recharge(BigDecimal amount);

    /**
     * 当前登录用户余额扣款
     *
     * @param amount 扣款金额（元）
     * @return 扣款后余额
     */
    BalanceInfoRespDTO pay(BigDecimal amount);
}
