package org.openzjl.index12306.biz.ticketservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.openzjl.index12306.biz.ticketservice.dao.entity.SeatDO;

import java.util.List;

/**
 * 座位接口层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:29
 */
public interface SeatService extends IService<SeatDO> {

    /**
     * 获取列车车厢中可用的座位集合
     * @param trainId        列车ID
     * @param carriageNumber 车厢号
     * @param seatType       座位类型
     * @param departure      出发站
     * @param arrival        到达站
     * @return               可用座位集合
     */
    List<String> listAvailableSeat(String trainId, String carriageNumber, Integer seatType, String departure, String arrival);

    /**
     * 获取列车车厢余票集合
     * @param trainId           列车ID
     * @param departure         出发站
     * @param arrival           到达站
     * @param trainCarriageList 车厢编号集合
     * @return                  车厢余票集合
     */
    List<Integer> listSeatRemainingTicket(String trainId, String departure, String arrival, List<String> trainCarriageList);

    /**
     * 查询列车有余票的车厢号集合
     * @param trainId       列车ID
     * @param carriageType  车厢类型
     * @param departure     出发站
     * @param arrival       到达站
     * @return              车厢号集合
     */
    List<String> listUsableCarriageNumber(String trainId, Integer carriageType, String departure, String arrival);


    // TODO
}
