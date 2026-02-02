package org.openzjl.index12306.biz.payservice.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.biz.payservice.common.enums.RefundTypeEnum;
import org.openzjl.index12306.biz.payservice.remote.dto.TicketOrderPassengerDetailRespDTO;

import java.util.List;

/**
 * 退款结果回调订单服务事件
 *
 * @author zhangjlk
 * @date 2026/1/30 15:42
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResultCallBackOrderEvent {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 退款类型
     */
    private RefundTypeEnum refundType;

    /**
     * 部分车票退款详情
     */
    private List<TicketOrderPassengerDetailRespDTO> partialRefundTicketDetailList;
}
