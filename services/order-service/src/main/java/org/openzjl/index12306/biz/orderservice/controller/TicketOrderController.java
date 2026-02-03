/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.orderservice.dto.req.*;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.biz.orderservice.service.OrderService;
import org.openzjl.index12306.framework.starter.convention.page.PageResponse;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车票订单接口控制层
 *
 * @author zhangjlk
 * @date 2026/1/13 20:59
 */
@RestController
@RequiredArgsConstructor
public class TicketOrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    /**
     * 根据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return 车票订单
     */
    @GetMapping("/api/order-service/order/item/ticket/query")
    public Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn")  String orderSn) {
        return Results.success(orderService.queryTicketByOrderSn(orderSn));
    }

    /**
     * 根据子订单记录id查询车票子订单详情
     *
     * @param requestParam 请求参数
     * @return 车票子订单详情
     */
    @GetMapping("/api/order-service/order/ticket/query")
    public Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam) {
        return Results.success(orderItemService.queryTicketItemOrderById(requestParam));
    }

    /**
     * 分页查询车票订单
     *
     * @param requestParam 请求参数
     * @return 车票订单详情（分页结果）
     */
    @GetMapping("/api/order-service/order/ticket/page")
    public Result<PageResponse<TicketOrderDetailRespDTO>> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        return Results.success(orderService.pageTicketOrder(requestParam));
    }

    /**
     * 分页查询本人车票订单
     *
     * @param requestParam 请求参数
     * @return 本人车票订单详情（分页结果）
     */
    @GetMapping("/api/order-service/order/ticket/self/page")
    public Result<PageResponse<TicketOrderDetailSelfRespDTO>> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        return Results.success(orderService.pageSelfTicketOrder(requestParam));
    }

    /**
     * 车票订单创建
     *
     * @param requestParam 请求参数
     * @return 车票订单创建结果
     */
    @PostMapping("/api/order-service/order/ticket/create")
    public Result<String> createTicketOrder(@RequestBody TicketOrderCreateReqDTO requestParam) {
        return Results.success(orderService.createTicketOrder(requestParam));
    }

    /**
     * 车票订单关闭
     *
     * @param requestParam 订单关闭请求参数
     * @return 订单关闭结果
     */
    @PostMapping("/api/order-service/order/ticket/close")
    public Result<Boolean> closeTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.closeTicketOrder(requestParam));
    }

    /**
     * 车票订单取消
     *
     * @param requestParam 订单取消请求参数
     * @return 订单取消结果
     */
    @PostMapping("/api/order-service/order/ticket/cancel")
    public Result<Boolean> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.cancelTicketOrder(requestParam));
    }
}
