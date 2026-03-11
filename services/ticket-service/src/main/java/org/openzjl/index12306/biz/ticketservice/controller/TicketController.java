/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.TicketService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车票控制层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:26
 */
@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * 分页查询车票（前端 /api/ticket-service/ticket/query 调用）
     * 使用 V1：缓存为空时会从 DB 加载并回填 Redis（站点→地区映射、地区对车次列表），避免查不到车次。
     */
    @GetMapping("/api/ticket-service/ticket/query")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQueryV1(requestParam));
    }

    /**
     * 购买车票（高性能版本）
     * 前端确认下单：POST /api/ticket-service/ticket/purchase/v2
     */
    @PostMapping("/api/ticket-service/ticket/purchase/v2")
    public Result<TicketPurchaseRespDTO> purchaseTicketV2(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV2(requestParam));
    }
}
