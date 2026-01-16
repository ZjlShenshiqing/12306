package org.openzjl.index12306.biz.orderservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 本人车票订单详情返回参数
 *
 * @author zhangjlk
 * @date 2026/1/15 15:53
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketOrderDetailSelfRespDTO {

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
     * 车票类型
     */
    private Integer ticketType;

    /**
     * 订单金额
     */
    private Integer amount;
}
