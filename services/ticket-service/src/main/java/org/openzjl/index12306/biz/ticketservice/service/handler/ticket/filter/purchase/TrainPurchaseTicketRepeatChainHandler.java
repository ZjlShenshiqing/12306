package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.springframework.stereotype.Component;

/**
 * 购票流程过滤器 - 验证乘客是否重复购买
 *
 * @author zhangjlk
 * @date 2026/3/2 下午4:27
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketRepeatChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO>{

    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {

    }

    @Override
    public int getOrder() {
        return 30;
    }
}
