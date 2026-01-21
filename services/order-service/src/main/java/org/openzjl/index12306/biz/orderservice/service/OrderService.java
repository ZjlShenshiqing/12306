package org.openzjl.index12306.biz.orderservice.service;

import org.openzjl.index12306.biz.orderservice.dto.req.CancelTicketOrderReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderSelfPageQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import org.openzjl.index12306.framework.starter.convention.page.PageResponse;

/**
 *
 * @author zhangjlk
 * @date 2026/1/14 11:35
 */
public interface OrderService {

    /**
     * 根据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return        车票订单
     */
    TicketOrderDetailRespDTO queryTicketByOrderSn(String orderSn);

    /**
     * 根据用户名分页查询车票订单
     *
     * @param requestParam 根据用户id分页查询对象
     * @return 订单分页详情
     */
    PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam);

    /**
     * 分页查询本人车票订单
     *
     * @param requestParam 请求参数
     * @return 本人车票订单详情（分页结果）
     */
    PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam);

    /**
     * 车票订单创建
     *
     * @param requestParam 请求参数
     * @return 车票订单创建结果
     */
    String createTicketOrder(TicketOrderCreateReqDTO requestParam);

    /**
     * 关闭车票订单
     *
     * @param requestParam 取消火车票入参
     * @return 结果
     */
    Boolean closeTicketOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 车票订单取消
     *
     * @param requestParam 订单取消请求参数
     * @return 订单取消结果
     */
    Boolean cancelTicketOrder(CancelTicketOrderReqDTO requestParam);


    void statusReversal();
}
