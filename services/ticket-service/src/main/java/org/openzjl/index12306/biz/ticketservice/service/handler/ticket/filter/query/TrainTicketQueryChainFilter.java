package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.filter.query;

import org.openzjl.index12306.biz.ticketservice.common.enums.TicketChainMarkEnum;
import org.openzjl.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.openzjl.index12306.framework.starter.designpattern.chain.AbstractChainHandler;

/**
 * 列车车票查询过滤器
 *
 * @author zhangjlk
 * @date 2026/3/2 下午4:32
 */
public interface TrainTicketQueryChainFilter<T extends TicketPageQueryReqDTO> extends AbstractChainHandler<TicketPageQueryReqDTO> {

    @Override
    default String mark() {
        return TicketChainMarkEnum.TRAIN_QUERY_FILTER.name();
    }
}
