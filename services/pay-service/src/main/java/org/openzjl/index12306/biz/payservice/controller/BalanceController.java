/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.payservice.dto.req.BalanceRechargeReqDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.BalanceInfoRespDTO;
import org.openzjl.index12306.biz.payservice.service.BalanceService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;

/**
 * 余额控制层
 */
@RestController
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    /**
     * 查询当前用户余额
     */
    @GetMapping("/api/pay-service/balance/info")
    public Result<BalanceInfoRespDTO> queryBalance() {
        return Results.success(balanceService.queryCurrentUserBalance());
    }

    /**
     * 余额充值
     */
    @PostMapping("/api/pay-service/balance/recharge")
    public Result<BalanceInfoRespDTO> recharge(@RequestBody BalanceRechargeReqDTO requestParam) {
        return Results.success(balanceService.recharge(requestParam.getAmount()));
    }
}
