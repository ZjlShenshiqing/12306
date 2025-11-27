package org.openzjl.index12306.biz.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.RegionStationService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地区以及车站查询控制层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:26
 */
@RestController
@RequiredArgsConstructor
public class RegionStationController {

    private final RegionStationService regionStationService;

    /**
     * 查询车站 - 城市站点集合信息
     */
    @GetMapping("/api/ticket-service/region-station/query")
    public Result<List<RegionStationQueryRespDTO>> listRegionStation(RegionStationQueryReqDTO regionStationQueryParam) {
        return Results.success(regionStationService.listRegionStation(regionStationQueryParam));
    }

    /**
     * 查询车站站点集合信息
     */
    @GetMapping("/api/ticket-service/station/all")
    public Result<List<StationQueryRespDTO>> listAllStation() {
        return Results.success(regionStationService.listAllStation());
    }
}
