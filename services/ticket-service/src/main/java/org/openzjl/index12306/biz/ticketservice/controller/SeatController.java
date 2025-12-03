package org.openzjl.index12306.biz.ticketservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dao.entity.SeatDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 座位控制器 - 用于测试环境使用，将指定车次的所有座位状态重置为Avaliable，也就是可用
 * 删除车次所有的余票、令牌桶缓存
 *
 * @author zhangjlk
 * @date 2025/11/22 16:28
 */
@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatMapper seatMapper;
    private final DistributedCache distributedCache;
    private final TrainStationMapper trainStationMapper;

    /**
     * 座位重置
     */
    @PostMapping("/api/ticket-service/temp/seat/reset")
    public Result<Void> purchaseTickets(@RequestParam String trainId) {
        SeatDO seatDO = new SeatDO();
//        seatDO.setSeatStatus();
        return Results.success();
    }
}
