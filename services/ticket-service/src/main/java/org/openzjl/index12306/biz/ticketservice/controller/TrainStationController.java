package org.openzjl.index12306.biz.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 列车站点控制层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:27
 */
@RestController
@RequiredArgsConstructor
public class TrainStationController {

    private final TrainStationService trainStationService;

    /**
     * 根据列车ID查询站点信息
     * @param trainId 列车ID
     * @return 站点信息
     */
    @GetMapping("/api/ticket-service/train-station/query")
    public Result<List<TrainStationQueryRespDTO>> listTrainStationQuery(String trainId) {
        return Results.success(trainStationService.listTrainStationQuery(trainId));
    }
}
