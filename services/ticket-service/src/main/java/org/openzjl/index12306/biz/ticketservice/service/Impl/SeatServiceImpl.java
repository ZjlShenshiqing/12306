/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import org.openzjl.index12306.biz.ticketservice.dao.entity.SeatDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.dto.domain.SeatTypeCountDTO;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_CARRIAGE_REMAINING_TICKET;

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

        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)

                // --- 拼接 WHERE 查询条件 ---
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

        // 数据转换
        // 数据库查出来的是 List<SeatDO> 对象，但方法要求返回 List<String> (只有座位号)
        return seatDOList.stream()
                .map(SeatDO::getSeatNumber)  // 提取每个对象中的 seatNumber 字段
                .collect(Collectors.toList()); // 收集成一个新的 String 列表
    }

    @Override
    public List<Integer> listSeatRemainingTicket(String trainId, String departure, String arrival, List<String> trainCarriageList) {
        // 构建缓存 Key 的后缀
        // 格式通常为：车次ID_出发站_到达站 (例如：G123_Beijing_Shanghai)
        String keySuffix = StrUtil.join("_", trainId, departure, arrival);

        // 检查缓存中是否存在该 Key
        // 这一步是为了防止缓存穿透，或者在没数据时直接跳过，避免后续无效操作
        if (distributedCache.hasKey(TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix)) {

            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

            // 使用 Hash 结构的 multiGet 进行批量查询
            // Redis 命令对应：HMGET key field1 field2 ...
            // key: 完整的缓存键
            // fields: trainCarriageList (车厢列表，例如 ["1", "2", "3"])，这里查询这些车厢对应的余票
            List<Object> trainStationCarriageRemainingTicket = stringRedisTemplate
                    .opsForHash()
                    .multiGet(TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix, Arrays.asList(trainCarriageList.toArray()));

            // 判空处理
            // 确保从 Redis 取回来的列表不为空，防止后续空指针异常
            if (CollUtil.isNotEmpty(trainStationCarriageRemainingTicket)) {

                // 数据转换与返回
                // Redis Hash 中存储的值通常是 String 类型，这里通过 Stream 流将其转换为 Integer
                // 最终返回一个整数列表，对应每个车厢的剩余票数
                return trainStationCarriageRemainingTicket.stream()
                        .map(each -> Integer.parseInt(each.toString())) // String 转 int
                        .collect(Collectors.toList());
            }
        }

        // 缓存未命中，兜底查询数据库
        SeatDO seatDO = SeatDO.builder()
                .trainId(Long.parseLong(trainId))
                .startStation(departure)
                .endStation(arrival)
                .build();
        return seatMapper.listSeatRemainingTicket(seatDO, trainCarriageList);
    }

    @Override
    public List<String> listUsableCarriageNumber(String trainId, Integer carriageType, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                // SQL: WHERE train_id = ?
                .eq(SeatDO::getTrainId, trainId)

                // SQL: AND seat_type = ? (例如：一等座、二等座)
                .eq(SeatDO::getSeatType, carriageType)

                // SQL: AND start_station = ? AND end_station = ?
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival)

                // SQL: AND seat_status = 0 (假设 0 代表 AVAILABLE/可用)
                // 只查找那些当前状态是“可售”或“未占用”的座位
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())

                // SQL: GROUP BY carriage_number
                // 【关键点】利用分组实现“去重”效果。
                // 我们不关心每个车厢剩多少张票，只关心“哪些车厢有票”。
                // 只要某个车厢号下有至少一条满足上述条件的记录，该车厢号就会被查出来。
                .groupBy(SeatDO::getCarriageNumber)

                // SQL: SELECT carriage_number
                .select(SeatDO::getCarriageNumber);

        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);
        return seatDOList.stream()
                .map(SeatDO::getCarriageNumber)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeatTypeCountDTO> listAvailableSeatTypeCount(Long trainId, String startStation, String endStation, List<Integer> seatTypes) {
        return seatMapper.listSeatTypeCount(trainId, startStation, endStation, seatTypes);
    }

    /**
     * 锁定座位
     * <p>
     * 当用户购买车票时，需要将座位状态从 AVAILABLE（可用）更新为 LOCKED（锁定）
     * 锁定后，该座位在支付完成前不能被其他用户购买
     * <p>
     * 业务场景说明：
     * 假设列车路线：["北京", "天津", "济南", "南京"]
     * 用户购买：天津 → 南京 的票，座位号：1号车厢1A
     * <p>
     * 需要锁定的路线：
     * 1. 北京 → 南京（因为如果北京有人买票到南京，这个座位在天津→南京段已被占用）
     * 2. 天津 → 济南（实际乘坐的第一段）
     * 3. 天津 → 南京（实际乘坐的完整路线）
     * 4. 济南 → 南京（实际乘坐的第二段）
     * <p>
     * 所以需要对每个受影响的路线，都锁定这个座位，确保不会超卖
     * <p>
     *
     * @param trainId                     列车 ID
     * @param departure                   出发站
     * @param arrival                     到达站
     * @param trainPurchaseTicketRespList 购票响应列表，包含已分配的座位信息（车厢号、座位号等）
     */
    @Override
    public void lockSeat(String trainId, String departure, String arrival, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketRespList) {

        // 计算需要扣减余票的所有路线
        List<RouteDTO> routeList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);

        // 外层循环：遍历每个购票的乘客/座位（可能一次买多张票）
        // 内层循环：遍历每个受影响的路线（一个座位可能影响多个路线）
        trainPurchaseTicketRespList.forEach(each -> {
            // each: 单个购票响应，包含车厢号、座位号等信息
            routeList.forEach(item -> {
                // item: 单个受影响的路线，包含起始站和终点站

                // 构建更新条件：精确匹配要锁定的座位
                // 条件包括：车次、车厢号、起始站、终点站、座位号
                LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                        .eq(SeatDO::getTrainId, trainId)                                    // 车次ID
                        .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())            // 车厢号（如：1号车厢）
                        .eq(SeatDO::getStartStation, item.getStartStation())                // 路线的起始站
                        .eq(SeatDO::getEndStation, item.getEndStation())                    // 路线的终点站
                        .eq(SeatDO::getSeatNumber, each.getSeatNumber());                  // 座位号（如：1A）

                // 构建更新对象：将座位状态更新为 LOCKED（锁定）
                SeatDO updateSeatDO = SeatDO.builder()
                        .seatStatus(SeatStatusEnum.LOCKED.getCode())
                        .build();

                // 执行更新：将该座位在指定路线上的状态更新为锁定
                // 注意：同一个座位可能对应多条路线，每条路线都需要单独更新
                seatMapper.update(updateSeatDO, updateWrapper);
            });
        });
    }

    /**
     * 解锁座位
     * <p>
     * 当订单取消或长时间未支付时，需要将座位状态从 LOCKED（锁定）恢复为 AVAILABLE（可用）
     * 解锁后，该座位可以重新被其他用户购买
     * <p>
     * 业务场景说明：
     * 1. 订单取消：用户主动取消订单，需要释放已锁定的座位
     * 2. 超时未支付：订单创建后超过支付时间未支付，系统自动取消订单并释放座位
     * <p>
     * 解锁逻辑与锁定逻辑对应：
     * 假设之前购买"天津 → 南京"的票时，锁定了以下路线的座位：
     * - 北京 → 南京
     * - 天津 → 济南
     * - 天津 → 南京
     * - 济南 → 南京
     * <p>
     * 取消订单时，需要解锁所有这些路线的座位，确保座位可以重新被购买
     * <p>
     * 注意：解锁操作需要与锁定操作保持一致，即锁定时锁定了哪些路线，
     * 解锁时也要解锁相同的路线，否则可能导致座位状态不一致
     *
     * @param trainId                   列车 ID
     * @param departure                 出发站
     * @param arrival                   到达站
     * @param trainPurchaseTicketResults 购票响应列表，包含需要解锁的座位信息（车厢号、座位号等）
     */
    @Override
    public void unLock(String trainId, String departure, String arrival, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults) {

        // 计算需要解锁的所有路线
        List<RouteDTO> routeList = trainStationService.listTakeoutTrainStationRoute(trainId, departure, arrival);

        // 外层循环：遍历每个需要解锁的座位（可能一次取消多张票）
        // 内层循环：遍历每个受影响的路线（一个座位可能影响多个路线）
        trainPurchaseTicketResults.forEach(each -> {
            // each: 单个购票响应，包含车厢号、座位号等信息
            routeList.forEach(item -> {
                // item: 单个受影响的路线，包含起始站和终点站

                // 条件必须与锁定时的条件完全一致
                LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                        .eq(SeatDO::getTrainId, trainId)                                    // 车次ID
                        .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())            // 车厢号（如：1号车厢）
                        .eq(SeatDO::getStartStation, item.getStartStation())                // 路线的起始站
                        .eq(SeatDO::getEndStation, item.getEndStation())                    // 路线的终点站
                        .eq(SeatDO::getSeatNumber, each.getSeatNumber());                  // 座位号（如：1A）

                // 构建更新对象：将座位状态更新为 AVAILABLE（可用）
                SeatDO updateSeatDO = SeatDO.builder()
                        .seatStatus(SeatStatusEnum.AVAILABLE.getCode())
                        .build();

                // 执行更新：将该座位在指定路线上的状态更新为可用
                // 注意：同一个座位可能对应多条路线，每条路线都需要单独更新
                // 解锁后，这些路线的座位可以重新被其他用户购买
                seatMapper.update(updateSeatDO, updateWrapper);
            });
        });
    }
}
