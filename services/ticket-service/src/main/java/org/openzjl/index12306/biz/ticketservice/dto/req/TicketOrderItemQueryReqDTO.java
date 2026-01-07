package org.openzjl.index12306.biz.ticketservice.dto.req;

import lombok.Data;

import java.util.List;

/**
 * 车票子订单查询
 *
 * @author zhangjlk
 * @date 2026/1/7 14:30
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
    private List<String> orderItemRecordIds;
}
