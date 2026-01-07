package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;

/**
 * 选择座位实体
 *
 * @author zhangjlk
 * @date 2025/12/31 上午11:55
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelectSeatDTO {

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 座位对应的乘车人集合
     */
    private List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails;

    /**
     * 购票请求参数
     */
    private PurchaseTicketReqDTO requestParam;
}
