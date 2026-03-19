/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.payservice.convert.PayRequestConvert;
import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.biz.payservice.dto.command.PayCommand;
import org.openzjl.index12306.biz.payservice.dto.resp.PayInfoRespDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.PayRespDTO;
import org.openzjl.index12306.biz.payservice.service.PayService;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;

/**
 * 鏀粯鎺у埗灞? */
@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    @PostMapping("/api/pay-service/pay/create")
    public Result<PayRespDTO> pay(@RequestBody PayCommand requestParam) {
        PayRequest payRequest = PayRequestConvert.command2PayRequest(requestParam);
        if (payRequest == null) {
            throw new ServiceException("unsupported pay channel");
        }
        PayRespDTO result = payService.commonPay(payRequest);
        return Results.success(result);
    }

    @GetMapping("/api/pay-service/pay/query/order-sn")
    public Result<PayInfoRespDTO> getPayInfoByOrderSn(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(payService.getPayInfoByOrderSn(orderSn));
    }

    @GetMapping("/api/pay-service/pay/query/pay-sn")
    public Result<PayInfoRespDTO> getPayInfoByPaySn(@RequestParam(value = "paySn") String paySn) {
        return Results.success(payService.getPayInfoByPaySn(paySn));
    }
}
