package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.filter.refund;

import org.openzjl.index12306.biz.ticketservice.common.enums.TicketChainMarkEnum;
import org.openzjl.index12306.biz.ticketservice.dto.req.RefundTicketReqDTO;
import org.openzjl.index12306.framework.starter.designpattern.chain.AbstractChainHandler;

/**
 * 列车车票退款过滤器
 *
 * @author zhangjlk
 * @date 2026/3/3 上午10:01
 */
public interface TrainRefundTicketChainFilter<T extends RefundTicketReqDTO> extends AbstractChainHandler<RefundTicketReqDTO> {

        @Override
        default String mark() {
            return TicketChainMarkEnum.TRAIN_REFUND_TICKET_FILTER.name();
        }
}
