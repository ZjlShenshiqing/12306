package org.openzjl.index12306.biz.ticketservice.dto.req;

import lombok.Data;

/**
 * 购票乘车人详情实体
 *
 * @author zhangjlk
 * @date 2025/12/13 下午3:21
 */
@Data
public class PurchaseTicketPassengerDetailDTO {

    /**
     * 乘车人ID
     */
    private String passengerId;

    /**
     * 座位类型
     */
    private Integer seatType;
}
