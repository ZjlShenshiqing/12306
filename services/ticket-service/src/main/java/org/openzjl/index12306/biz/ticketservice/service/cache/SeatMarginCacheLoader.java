/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.service.cache;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import org.openzjl.index12306.biz.ticketservice.dao.entity.SeatDO;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.cache.toolkit.CacheUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.openzjl.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * 座位余量缓存加载
 *
 * @author zhangjlk
 * @date 2025/12/25 下午9:50
 */
@Component
@RequiredArgsConstructor
public class SeatMarginCacheLoader {

    private final TrainMapper trainMapper;
    private final SeatMapper seatMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final TrainStationService trainStationService;

    /**
     * 加载座位余量缓存（从数据库查询并写入Redis）
     * <p>
     * 该方法用于在缓存未命中时，从数据库查询座位余票信息并加载到Redis缓存中。
     * 使用分布式锁确保高并发场景下只有一个线程执行数据库查询和缓存写入操作。
     * <p>
     * 工作流程：
     * 1. 构建缓存Key，检查缓存是否存在
     * 2. 如果缓存不存在，获取分布式锁
     * 3. 从数据库查询列车信息和路线信息
     * 4. 根据列车类型查询不同座位类型的余票数量
     * 5. 将所有路线和座位类型的余票信息批量写入Redis
     * 6. 返回查询的路线对应的余票信息
     * <p>
     * 列车类型与座位类型对应关系：
     * - 类型0（高速铁路）：商务座(0)、一等座(1)、二等座(2)
     * - 类型1（动车）：二等包座(3)、一等卧(4)、二等卧(5)、无座(13)
     * - 类型2（普通车）：软卧(6)、硬卧(7)、硬座(8)、无座(13)
     * <p>
     * 缓存结构：
     * - Redis Hash结构，Key格式：TRAIN_STATION_REMAINING_TICKET + 车次ID_出发站_到达站
     * - Hash的Field：座位类型编码（如："0"、"1"、"2"）
     * - Hash的Value：余票数量（字符串格式，如："10"）
     *
     * @param trainId   车次ID，不能为null（如："G123"）
     * @param seatType  座位类型编码（字符串格式），用于检查缓存是否存在
     * @param departure 出发站编码，不能为null（如："1001"）
     * @param arrival   到达站编码，不能为null（如："2001"）
     * @return 返回指定路线的座位余票信息Map，Key为座位类型编码，Value为余票数量（字符串格式）
     *         如果缓存已存在或查询失败，返回空Map
     */
    public Map<String, String> load(String trainId, String seatType, String departure, String arrival) {
        // 初始化结果Map，用于存储所有路线和座位类型的余票信息
        // Key: Redis缓存Key，Value: 该路线所有座位类型的余票信息Map
        Map<String, Map<String, String>> trainStationRemainingTicketMaps = new LinkedHashMap<>();
        
        // 构建缓存Key后缀：车次ID_出发站_到达站
        // 例如："G123_1001_2001"
        String keySuffix = CacheUtil.buildKey(trainId, departure, arrival);
        
        // 获取分布式锁，防止多个线程同时查询数据库并写入缓存（防止缓存击穿）
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET, keySuffix));
        lock.lock();
        try {
            // 获取Redis操作模板
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            
            // 检查缓存是否存在
            // 从Redis Hash中获取指定座位类型的余票数量
            Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
            
            // 如果缓存不存在（quantityObj为null或空），需要从数据库加载
            if (CacheUtil.isNullOrBlank(quantityObj)) {
                // 获取列车基本信息
                // 从缓存或数据库获取列车信息，用于判断列车类型和计算路线
                TrainDO trainDO = distributedCache.safeGet(
                        TRAIN_INFO + trainId,
                        TrainDO.class,
                        () -> trainMapper.selectById(trainId),
                        ADVANCE_TICKET_DAY,
                        TimeUnit.DAYS
                );
                
                // 计算列车的所有路线段
                // 获取该车次从起始站到终点站的所有路线段
                // 例如：如果列车路线是 A->B->C->D，则返回：A->B、A->C、A->D、B->C、B->D、C->D
                List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(trainId, trainDO.getStartStation(), trainDO.getEndStation());
                
                // 根据列车类型查询不同座位类型的余票
                if (CollUtil.isNotEmpty(routeDTOList)) {
                    // 根据列车类型（0=高速铁路，1=动车，2=普通车）查询对应的座位类型
                    switch (trainDO.getTrainType()) {
                        // 高速铁路（类型0）：支持商务座、一等座、二等座
                        case 0 -> {
                            // 遍历所有路线段，查询每种座位类型的余票
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                // 查询商务座（编码0）的余票数量
                                trainStationRemainingTicket.put("0", selectSeatMargin(trainId, 0, each.getStartStation(), each.getEndStation()));
                                // 查询一等座（编码1）的余票数量
                                trainStationRemainingTicket.put("1", selectSeatMargin(trainId, 1, each.getStartStation(), each.getEndStation()));
                                // 查询二等座（编码2）的余票数量
                                trainStationRemainingTicket.put("2", selectSeatMargin(trainId, 2, each.getStartStation(), each.getEndStation()));
                                
                                // 构建该路线段的缓存Key后缀
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                // 将余票信息存入Map，Key为完整的Redis缓存Key
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        // 动车（类型1）：支持二等包座、一等卧、二等卧、无座
                        case 1 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                // 查询二等包座（编码3）的余票数量
                                trainStationRemainingTicket.put("3", selectSeatMargin(trainId, 3, each.getStartStation(), each.getEndStation()));
                                // 查询一等卧（编码4）的余票数量
                                trainStationRemainingTicket.put("4", selectSeatMargin(trainId, 4, each.getStartStation(), each.getEndStation()));
                                // 查询二等卧（编码5）的余票数量
                                trainStationRemainingTicket.put("5", selectSeatMargin(trainId, 5, each.getStartStation(), each.getEndStation()));
                                // 查询无座（编码13）的余票数量
                                trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                        // 普通车（类型2）：支持软卧、硬卧、硬座、无座
                        case 2 -> {
                            for (RouteDTO each : routeDTOList) {
                                Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                                // 查询软卧（编码6）的余票数量
                                trainStationRemainingTicket.put("6", selectSeatMargin(trainId, 6, each.getStartStation(), each.getEndStation()));
                                // 查询硬卧（编码7）的余票数量
                                trainStationRemainingTicket.put("7", selectSeatMargin(trainId, 7, each.getStartStation(), each.getEndStation()));
                                // 查询硬座（编码8）的余票数量
                                trainStationRemainingTicket.put("8", selectSeatMargin(trainId, 8, each.getStartStation(), each.getEndStation()));
                                // 查询无座（编码13）的余票数量
                                trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, each.getStartStation(), each.getEndStation()));
                                
                                String actualKeySuffix = CacheUtil.buildKey(trainId, each.getStartStation(), each.getEndStation());
                                trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                            }
                        }
                    }
                } else {
                    // 如果没有路线信息，初始化默认值
                    // 如果路线列表为空，根据列车类型初始化所有座位类型的余票为0
                    Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                    // 获取该列车类型支持的所有座位类型，并将余票数量初始化为"0"
                    VehicleTypeEnum.findSeatTypeByCode(trainDO.getTrainType())
                            .forEach(each -> trainStationRemainingTicket.put(String.valueOf(each), "0"));
                    trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + keySuffix, trainStationRemainingTicket);
                }

                // 批量写入Redis缓存
                // 将所有路线和座位类型的余票信息批量写入Redis Hash
                // 这样后续查询就可以直接从缓存获取，无需访问数据库
                trainStationRemainingTicketMaps.forEach((cacheKey, cacheMap) -> stringRedisTemplate.opsForHash().putAll(cacheKey, cacheMap));
            }
        } finally {
            // 无论成功与否，都要释放锁，避免死锁
            lock.unlock();
        }
        
        // ========== 第五步：返回查询结果 ==========
        // 返回指定路线（trainId_departure_arrival）的座位余票信息
        // 如果缓存已存在或查询失败，返回空Map
        return Optional.ofNullable(trainStationRemainingTicketMaps.get(TRAIN_STATION_REMAINING_TICKET + keySuffix))
                .orElse(new LinkedHashMap<>());
    }


    /**
     * 查询指定路线和座位类型的可用座位数量（余票数量）
     * <p>
     * 该方法用于从数据库查询指定车次、座位类型、出发站、到达站的可用座位数量。
     * 通常用于缓存加载场景：当Redis缓存中缺少余票信息时，从数据库查询并加载到缓存。
     * <p>
     * 查询条件说明：
     * 1. 车次ID：指定查询哪个车次
     * 2. 座位类型：指定查询哪种座位（如：0=商务座，1=一等座，2=二等座）
     * 3. 座位状态：只查询可用状态的座位（SeatStatusEnum.AVAILABLE）
     * 4. 出发站：座位覆盖的起始站点
     * 5. 到达站：座位覆盖的终点站点
     * <p>
     * 使用场景：
     * - 缓存未命中时，从数据库查询实际余票数量
     * - 初始化余票缓存
     * - 刷新余票缓存数据
     * <p>
     * 返回值说明：
     * - 返回座位数量的字符串形式（如："10"表示10张票）
     * - 如果查询结果为null，返回"0"（表示没有可用座位）
     *
     * @param trainId   车次ID，不能为null（如："G123"）
     * @param type      座位类型编码，不能为null（如：0=商务座，1=一等座，2=二等座）
     * @param departure 出发站编码，不能为null（如："1001"）
     * @param arrival   到达站编码，不能为null（如："2001"）
     * @return 可用座位数量的字符串形式，如果没有可用座位则返回"0"
     */
    private String selectSeatMargin(String trainId, Integer type, String departure, String arrival) {
        // 构建数据库查询条件
        // 使用MyBatis-Plus的LambdaQueryWrapper构建类型安全的查询条件
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                // 条件1：车次ID必须匹配
                // SQL: WHERE train_id = ?
                .eq(SeatDO::getTrainId, trainId)
                // 条件2：座位类型必须匹配
                // SQL: AND seat_type = ?
                .eq(SeatDO::getSeatType, type)
                // 条件3：座位状态必须是可用状态
                // SQL: AND seat_status = 0 (假设0表示可用)
                // 只统计可用状态的座位，已售出或不可用的座位不计算在内
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                // 条件4：座位的起始站必须匹配
                // SQL: AND start_station = ?
                // 确保座位覆盖的起始站与查询条件一致
                .eq(SeatDO::getStartStation, departure)
                // 条件5：座位的终点站必须匹配
                // SQL: AND end_station = ?
                // 确保座位覆盖的终点站与查询条件一致
                .eq(SeatDO::getEndStation, arrival);
        
        // 执行查询并处理结果
        // selectCount() 方法执行 COUNT(*) 查询，返回符合条件的记录数量（Long类型）
        return Optional.ofNullable(seatMapper.selectCount(queryWrapper))
                // 将Long类型的数量转换为String类型
                // 例如：10L → "10"
                .map(String::valueOf)
                // 如果查询结果为null（理论上selectCount不会返回null，但为了安全起见），返回默认值"0"
                // 表示没有可用座位
                .orElse("0");
    }
}
