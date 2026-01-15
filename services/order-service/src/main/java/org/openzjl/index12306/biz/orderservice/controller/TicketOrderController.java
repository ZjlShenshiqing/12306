package org.openzjl.index12306.biz.orderservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.biz.orderservice.service.OrderService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @GetMapping
    public Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn")  String orderSn) {
        return Results.success(orderService.queryTicketByOrderSn(orderSn));
    }
}
