/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.service;

import org.openzjl.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.StationQueryRespDTO;

import java.util.List;

/**
 * 地区以及车站接口层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:29
 */
public interface RegionStationService {

    /**
     * 查询车站 & 城市站点集合信息
     *
     * @param regionStationParam 查询参数
     * @return                   车站 & 站点返回数据集合
     */
    List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO regionStationParam);

    /**
     * 查询所有车站集合信息
     * @return  车站返回数据集合
     */
    List<StationQueryRespDTO> listAllStation();
}
