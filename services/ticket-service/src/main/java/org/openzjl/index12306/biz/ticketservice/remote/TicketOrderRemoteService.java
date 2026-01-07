package org.openzjl.index12306.biz.ticketservice.remote;

import org.openzjl.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.TicketOrderItemQueryReqDTO;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderCreateRemoteReqDTO;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 车票订单远程服务调用
 *
 * @author zhangjlk
 * @date 2025/12/15 上午10:08
 */
@FeignClient(value = "index12306-order${unique-name:}-service", url = "http://127.0.0.1:9001")
public interface TicketOrderRemoteService {

    /**
     * 根据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return 列车订单记录
     */
    Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn);

    /**
     * 根据子订单记录id查询车票子订单详情
     *
     * @param requestParam 子订单记录id
     * @return 子订单详情
     */
    // @SpringQueryMap 注解会将对象参数转换为 URL 查询参数
    // 例如：GET /api/ticket-order/query-item?orderSn=ORDER123&orderItemRecordIds=ITEM1&orderItemRecordIds=ITEM2
    // 这样可以将对象的所有字段自动转换为查询参数，无需手动拼接 URL
    Result<List<TicketOrderPassengerDetailRespDTO>> queryTicketItemOrderById(@SpringQueryMap TicketOrderItemQueryReqDTO requestParam);

    /**
     * 车票订单创建
     *
     * @param requestParam 创建车票订单请求参数
     * @return 订单创建返回结果
     */
    @GetMapping("/api/order-service/order/ticket/create")
    Result<String> createTicketOrder(@RequestBody TicketOrderCreateRemoteReqDTO requestParam);



    /**
     * 车票订单取消
     *
     * @param requestParam 车票订单取消入参
     * @return 订单取消返回结果
     */
    Result<Void> cancelTicketOrder(@RequestParam CancelTicketOrderReqDTO requestParam);
}
