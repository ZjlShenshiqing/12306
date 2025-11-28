package org.openzjl.index12306.biz.ticketservice.service;

import org.openzjl.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 列车站点接口层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:29
 */
@Service
public interface TrainStationService {

    /**
     * 根据列车 ID 查询站点信息
     * @param trainId 列车ID
     * @return 列车经停站信息
     */
    List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId);
}
