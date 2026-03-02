package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.filter.query;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 列车车票查询过滤器 - 基础数据验证
 *
 * @author zhangjlk
 * @date 2026/3/2 下午4:36
 */
@Component
@RequiredArgsConstructor
public class TrainTicketQueryParamBaseVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO>{


    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        if (requestParam.getDepartureDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now())) {
            throw new ClientException("出发日期不能小于当前日期");
        }
        if (Objects.equals(requestParam.getFromStation(), requestParam.getToStation())) {
            throw new ClientException("出发地和目的地不能相同");
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
