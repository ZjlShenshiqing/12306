package org.openzjl.index12306.biz.ticketservice.service;

import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 列车站点接口层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:29
 */
public interface TrainStationService {

    /**
     * 根据列车 ID 查询站点信息
     * @param trainId 列车ID
     * @return 列车经停站信息
     */
    List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId);

    /**
     * 计算列车站点路线关系
     * 获取开始站点/目的站点/中间站点信息
     * 假设购买了深圳北到广州南到车，这个车是从香港西九龙发出的
     * 一共有香港 深圳 广州这三个站点
     * 那么方法返回
     * 香港西九龙 -> 广州南
     * 深圳北 -> 广州南
     *
     * @param trainId   列车ID
     * @param departure 出发站
     * @param arrival   到达站
     * @return          列车站点路线关系信息
     */
    List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival);

    /**
     * 计算需要扣减余票的站点
     * 假设购买了深圳北到广州南到车，这个车是从香港西九龙发出的
     * 那么这个座位的两个段都需要进行扣减
     * 香港西九龙 -> 广州南
     * 深圳北 -> 广州南
     *
     * @param trainId   列车ID
     * @param departure
     * @param arrival
     * @return
     */
    List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival);
}
