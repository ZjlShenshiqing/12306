package org.openzjl.index12306.biz.orderservice.dto.resp;

import cn.crane4j.annotation.Disassemble;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 车票订单详情返回参数
 *
 * @author zhangjlk
 * @date 2026/1/14 11:42
 */
@Data
public class TicketOrderDetailRespDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 车次ID
     */
    private Long trainId;

    /**
     * 出发地
     */
    private String departure;

    /**
     * 到达地
     */
    private String arrival;

    /**
     * 乘车日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private String ridingDate;

    /**
     * 订单创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date orderTime;

    /**
     * 车次号
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
     * 乘车人订单详情
     */
    @Disassemble
    private List<TicketOrderPassengerDetailRespDTO> passengerDetails;
}
