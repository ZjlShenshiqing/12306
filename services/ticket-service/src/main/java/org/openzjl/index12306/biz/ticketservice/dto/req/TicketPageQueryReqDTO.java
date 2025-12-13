package org.openzjl.index12306.biz.ticketservice.dto.req;

import lombok.Data;
import org.openzjl.index12306.framework.starter.convention.page.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 车票分页查询请求参数
 *
 * @author zhangjlk
 * @date 2025/12/12 上午9:56
 */
@Data
public class TicketPageQueryReqDTO extends PageRequest {

    /**
     * 出发地
     */
    private String fromStation;

    /**
     * 目的地
     */
    private String toStation;

    /**
     * 出发日期
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date departureDate;

    /**
     * 出发站点
     */
    private Date departure;

    /**
     * 到达站点
     */
    private String arrival;
}
