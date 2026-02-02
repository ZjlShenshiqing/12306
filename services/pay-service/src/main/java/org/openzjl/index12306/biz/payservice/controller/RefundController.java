package org.openzjl.index12306.biz.payservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.payservice.dto.req.RefundReqDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.RefundRespDTO;
import org.openzjl.index12306.biz.payservice.service.RefundService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款控制层
 *
 * @author zhangjlk
 * @date 2026/1/27 12:21
 */
@RestController
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    public Result<RefundRespDTO> refund(@RequestBody RefundReqDTO requestParam) {
        return Results.success(refundService.commonRefund(requestParam));
    }
}
