package org.openzjl.index12306.biz.ticketservice.dto.req;

import lombok.Data;
import org.openzjl.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;

import java.util.List;

/**
 * 购票请求入参
 *
 * @author zhangjlk
 * @date 2025/12/12 上午10:06
 */
@Data
public class PurchaseTicketReqDTO {

    /**
     * 车次ID
     */
    private String trainId;

    /**
     * 乘车人
     */
    private List<PurchaseTicketPassengerDetailDTO> passengers;

    /**
     * 选择座位
     */
    private List<String> chooseSeats;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;
}
