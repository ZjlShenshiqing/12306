package org.openzjl.index12306.biz.ticketservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import org.openzjl.index12306.biz.ticketservice.dao.entity.SeatDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.SeatTypeCountDTO;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 座位接口层实现
 *
 * @author zhangjlk
 * @date 2025/12/5 上午10:14
 */
@Service
@RequiredArgsConstructor
public class SeatServiceImpl extends ServiceImpl<SeatMapper, SeatDO> implements SeatService {

    private final SeatMapper seatMapper;
    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;

    @Override
    public List<String> listAvailableSeat(String trainId, String carriageNumber, Integer seatType, String departure, String arrival) {

        // 1. 构建查询构造器 (LambdaQueryWrapper)
        // 这里的 SeatDO.class 指定了我们要查询的数据库实体表
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)

                // --- 拼接 WHERE 查询条件 (也就是 SQL 中的 AND ...) ---
                // SQL: WHERE train_id = ?
                .eq(SeatDO::getTrainId, trainId)
                // SQL: AND carriage_number = ?
                .eq(SeatDO::getCarriageNumber, carriageNumber)
                // SQL: AND seat_type = ? (比如 0表示二等座, 1表示一等座)
                .eq(SeatDO::getSeatType, seatType)
                // SQL: AND start_station = ?
                .eq(SeatDO::getStartStation, departure)
                // SQL: AND end_station = ?
                .eq(SeatDO::getEndStation, arrival)

                // SQL: AND seat_status = 0 (假设 0 代表 AVAILABLE/可用)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())

                // --- 性能优化：只查询需要的列 ---
                // 我们只需要座位号(比如 "1A", "2B")，不需要把整个 SeatDO 对象的所有字段(创建时间、ID等)都查出来
                // SQL: SELECT seat_number ...
                .select(SeatDO::getSeatNumber);

        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);

        // 数据转换 (Stream 流处理)
        // 数据库查出来的是 List<SeatDO> 对象，但方法要求返回 List<String> (只有座位号)
        return seatDOList.stream()
                .map(SeatDO::getSeatNumber)  // 提取每个对象中的 seatNumber 字段
                .collect(Collectors.toList()); // 收集成一个新的 String 列表
    }

    @Override
    public List<Integer> listSeatRemainingTicket(String trainId, String departure, String arrival, List<String> trainCarriageList) {
        return List.of();
    }

    @Override
    public List<String> listUsableCarriageNumber(String trainId, Integer carriageType, String departure, String arrival) {
        return List.of();
    }

    @Override
    public List<SeatTypeCountDTO> listAvailableSeatTypeCount(Long trainId, String startStation, String endStation, List<Integer> seatTypes) {
        return List.of();
    }

    @Override
    public void lockSeat(String trainId, String departure, String arrival, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketRespDTOList) {

    }

    @Override
    public void unLock(String trainId, String departure, String arrival, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults) {

    }
}
