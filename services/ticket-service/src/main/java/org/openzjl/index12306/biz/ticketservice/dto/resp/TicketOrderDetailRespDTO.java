package org.openzjl.index12306.biz.ticketservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;

import java.util.Date;
import java.util.List;

/**
 * 乘车人车票订单详情返回参数
 *
 * @author zhangjlk
 * @date 2025/12/5 上午9:47
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketOrderDetailRespDTO {

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 车厢号
     */
    private String carriageNumber;

    /**
     * 座位号
     */
    private String seatNumber;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;

    /**
     * 车票类型
     */
    private Integer ticketType;

    /**
     * 订单金额
     */
    private Integer amount;
}
