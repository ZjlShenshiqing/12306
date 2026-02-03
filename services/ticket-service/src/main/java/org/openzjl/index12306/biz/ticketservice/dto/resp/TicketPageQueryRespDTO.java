/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.domain.TicketListDTO;

import java.util.List;

/**
 * 车票分页查询响应参数
 *
 * @author zhangjlk
 * @date 2025/12/10 上午10:55
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketPageQueryRespDTO {

    /**
     * 车次车票集合
     */
    private List<TicketListDTO> ticketList;

    /**
     * 车次类型
     * D - 动车
     * Z - 直达
     * 复兴号等等
     */
    private List<Integer> trainBrandList;

    /**
     * 出发车站
     */
    private List<String> departureStationList;

    /**
     * 到达车站
     */
    private List<String> arrivalStationList;

    /**
     * 车次席别
     */
    private List<Integer> seatClassTypeList;
}
