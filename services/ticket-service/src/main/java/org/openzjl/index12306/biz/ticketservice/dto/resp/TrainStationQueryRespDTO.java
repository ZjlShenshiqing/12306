package org.openzjl.index12306.biz.ticketservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 列车站点查询响应参数
 *
 * @author zhangjlk
 * @date 2025/11/28 09:56
 */
@Data
public class TrainStationQueryRespDTO {

    /**
     * 站序
     */
    private String sequence;

    /**
     * 站名
     */
    private String departure;

    /**
     * 到站时间
     */
    // 这个时间字段在转成 JSON 时，用“小时:分钟”的 24 小时格式输出，并且按东八区时区来计算和解析
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

    /**
     * 出发时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 停留时间
     */
    private Integer stopoverTime;
}
