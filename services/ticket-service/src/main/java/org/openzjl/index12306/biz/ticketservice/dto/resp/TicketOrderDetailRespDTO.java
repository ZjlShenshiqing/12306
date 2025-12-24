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
     * 订单号
     */
    private String orderSn;

    /**
     * 列车ID
     */
    private Long trainId;

    /**
     * 出发站
     */
    private String departure;

    /**
     * 到达站
     */
    private String arrival;

    /**
     * 乘车日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date ridingDate;

    /**
     * 订单时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date orderTime;

    /**
     * 列车编号
     */
    private String trainNumber;

    /**
     * 出发时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 到达时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

    /**
     * 乘车人订单详情列表
     */
    private List<TicketOrderPassengerDetailRespDTO> passengerDetails;
}
