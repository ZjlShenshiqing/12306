package org.openzjl.index12306.biz.orderservice.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderItemCreateReqDTO;

import java.util.List;

/**
 * 延迟关闭订单事件
 *
 * @author zhangjlk
 * @date 2026/1/20 11:40
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayCloseOrderEvent {

    /**
     * 车次ID
     */
    private String trainId;

    /**
     * 出发地
     */
    private String departure;

    /**
     * 到达地
     */
    private String arrival;

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 乘车人购票订单详情
     */
    private List<TicketOrderItemCreateReqDTO> trainPurchaseTicketResults;
}
