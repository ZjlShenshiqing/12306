package org.openzjl.index12306.biz.orderservice.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 车票子订单查询
 *
 * @author zhangjlk
 * @date 2026/1/15 14:48
 */
@Data
public class TicketOrderItemQueryReqDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 子订单记录id
     */
    private List<Long> orderItemRecordIds;
}
