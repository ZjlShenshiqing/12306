package org.openzjl.index12306.biz.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.dao.entity.*;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.*;
import org.openzjl.index12306.biz.ticketservice.dto.domain.SeatClassDTO;
import org.openzjl.index12306.biz.ticketservice.dto.domain.TicketListDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.RefundTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import org.openzjl.index12306.biz.ticketservice.enums.TicketChainMarkEnum;
import org.openzjl.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.RefundTicketRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import org.openzjl.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.TicketService;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import org.openzjl.index12306.biz.ticketservice.toolkit.DateUtil;
import org.openzjl.index12306.biz.ticketservice.toolkit.TimeStringComparator;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.cache.toolkit.CacheUtil;
import org.openzjl.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.openzjl.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.*;
import static org.openzjl.index12306.biz.ticketservice.toolkit.DateUtil.convertDateToLocalTime;

/**
 * 车票接口实现
 *
 * @author zhangjlk
 * @date 2025/12/13 下午3:57
 * @description TicketServiceImpl
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, TicketDO> implements TicketService, CommandLineRunner {

    private final TrainMapper trainMapper;
    private final DistributedCache distributedCache;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final StationMapper stationMapper;
    private final SeatService seatService;
    private final TrainStationService trainStationService;
    private final RedissonClient redissonClient;
    private final ConfigurableEnvironment configurableEnvironment;
    private final AbstractChainContext<TicketPageQueryReqDTO> ticketPageQueryAbstractChainContext;
    private final AbstractChainContext<PurchaseTicketReqDTO> purchaseTicketAbstractChainContext;
    private final AbstractChainContext<RefundTicketReqDTO> refundTicketAbstractChainContext;
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private TicketService ticketService;

    @Value("${ticket.availability.cache-update.type}")
    private String ticketAvailabilityCacheUpdateType;

    @Value("${framework.cache.redis.prefix}")
    private String cacheRedisPrefix;


    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV1(TicketPageQueryReqDTO requestParam) {
        // 执行责任链处理
        // 触发车票查询责任链，执行一系列前置处理（如参数校验、权限校验等）
        ticketPageQueryAbstractChainContext.handler(
                TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(),
                requestParam
        );
        
        // 获取Redis操作模板
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        
        // 第一次检查缓存（无锁快速路径）
        // 从Redis Hash中批量获取站点信息
        // REGION_TRAIN_STATION_MAPPING: Hash的Key，存储站点编码到地区名称的映射
        // 查询字段：出发站编码和到达站编码
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
        
        // 统计缓存未命中的数量（返回null表示该站点信息不在缓存中）
        long count = stationDetails.stream().filter(Objects::isNull).count();
        
        // 双重检查锁定
        // 如果缓存未命中（count > 0），说明缓存中缺少某些站点信息，需要从数据库加载
        // 使用分布式锁确保高并发场景下只有一个线程执行数据库查询和缓存写入操作
        if (count > 0) {
            // 获取分布式锁，防止多个线程同时查询数据库并写入缓存（防止缓存击穿）
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION_MAPPING);
            lock.lock();
            try {
                // 第二次检查缓存（加锁后再次检查）
                // 双重检查：获取锁后再次检查缓存，可能其他线程已经加载完成
                stationDetails = stringRedisTemplate.opsForHash()
                        .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
                count = stationDetails.stream().filter(Objects::isNull).count();
                
                // 从数据库加载并写入缓存
                // 如果第二次检查仍然有缓存未命中，说明确实需要从数据库加载
                if (count > 0) {
                    // 从数据库查询所有站点信息（全量查询，用于构建完整的站点映射缓存）
                    List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());
                    
                    // 构建站点编码到地区名称的映射Map
                    // Key: 站点编码（如 "1001"）
                    // Value: 地区名称（如 "华北"）
                    Map<String, String> regionTrainStationMap = new HashMap<>();
                    stationDOList.forEach(each ->
                        regionTrainStationMap.put(each.getCode(), each.getRegionName())
                    );
                    
                    // 将完整的站点映射关系批量写入Redis Hash
                    // 这样后续查询就可以直接从缓存获取，无需访问数据库
                    stringRedisTemplate.opsForHash().putAll(REGION_TRAIN_STATION_MAPPING, regionTrainStationMap);
                    
                    // 构建本次查询的返回结果
                    // 从刚构建的映射Map中获取本次查询需要的站点信息
                    stationDetails = new ArrayList<>();
                    stationDetails.add(regionTrainStationMap.get(requestParam.getFromStation()));
                    stationDetails.add(regionTrainStationMap.get(requestParam.getToStation()));
                }
            } finally {
                // 无论成功与否，都要释放锁，避免死锁
                lock.unlock();
            }
        }

        // 查询车票列表（按地区查询）
        // 初始化车票结果列表，用于存储查询到的车票信息
        // 用户查询，列出所有适合的车次
        List<TicketListDTO> ticketResult = new ArrayList<>();
        
        // 构建地区列车站点缓存的Redis Hash Key
        // 格式：REGION_TRAIN_STATION + 出发地区 + 到达地区
        // 例如：index12306-ticket-service:region_train_station:华北:华东
        // stationDetails.get(0) 是出发站所在地区，stationDetails.get(1) 是到达站所在地区
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION, stationDetails.get(0), stationDetails.get(1));
        
        // 第一次检查缓存
        // 从Redis Hash中获取该地区对的所有车票信息
        // entries() 方法获取Hash中的所有字段和值
        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
        
        // 双重检查锁定
        // 如果缓存为空，说明需要从数据库查询并加载到缓存
        if (MapUtil.isEmpty(regionTrainStationAllMap)) {
            // 获取分布式锁，防止多个线程同时查询数据库并写入缓存
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION);
            lock.lock();
            try {
                // 第二次检查缓存
                // 双重检查：获取锁后再次检查缓存，可能其他线程已经加载完成
                regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);
                
                // 如果第二次检查仍然为空，说明确实需要从数据库加载
                if (MapUtil.isEmpty(regionTrainStationAllMap)) {
                    // 从数据库查询列车站点关系
                    // 构建查询条件：根据出发地区和到达地区查询列车站点关系
                    // 例如：查询从"华北"到"华东"的所有列车路线
                    LambdaQueryWrapper<TrainStationRelationDO> query = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion, stationDetails.get(0))  // 出发地区
                            .eq(TrainStationRelationDO::getEndRegion, stationDetails.get(1)); // 到达地区

                    // 执行数据库查询，获取所有符合条件的列车站点关系
                    List<TrainStationRelationDO> trainStationRelationList = trainStationRelationMapper.selectList(query);
                    
                    // 遍历查询结果，构建车票列表DTO
                    for (TrainStationRelationDO trainStationRelation : trainStationRelationList) {
                        // 从缓存或数据库获取列车基本信息
                        // 优先从缓存读取，缓存不存在则从数据库查询并写入缓存
                        TrainDO trainDO = distributedCache.safeGet(
                                TRAIN_INFO + trainStationRelation.getTrainId(),
                                TrainDO.class,
                                () -> trainMapper.selectById(trainStationRelation.getTrainId()),
                                ADVANCE_TICKET_DAY,
                                TimeUnit.DAYS);
                        
                        // 创建车票列表DTO对象，用于封装返回给前端的数据
                        TicketListDTO result = new TicketListDTO();
                        
                        // 设置车次基本信息
                        result.setTrainId(String.valueOf(trainDO.getId()));              // 车次ID
                        result.setTrainNumber(trainDO.getTrainNumber());                 // 车次号（如：G123）
                        
                        // 设置时间信息
                        // 出发时间：将Date转换为"HH:mm"格式（如：08:30）
                        result.setDepartureTime(convertDateToLocalTime(trainStationRelation.getDepartureTime(), "HH:mm"));
                        // 到达时间：将Date转换为"HH:mm"格式（如：14:30）
                        result.setArrivalTime(convertDateToLocalTime(trainStationRelation.getArrivalTime(), "HH:mm"));
                        // 运行时长：计算出发时间到到达时间的差值（如：06:00 表示6小时）
                        result.setDuration(DateUtil.calculateHourDifference(trainStationRelation.getDepartureTime(), trainStationRelation.getArrivalTime()));
                        
                        // 设置站点信息
                        result.setDeparture(trainStationRelation.getDeparture());        // 出发站编码
                        result.setArrival(trainStationRelation.getArrival());            // 到达站编码
                        result.setDepartureFlag(trainStationRelation.getDepartureFlag()); // 是否为始发站
                        result.setArrivalFlag(trainStationRelation.getArrivalFlag());    // 是否为终点站
                        
                        // 设置列车类型和品牌信息
                        result.setTrainType(trainDO.getTrainType());                     // 列车类型（如：高速铁路、动车等）
                        result.setTrainBrand(trainDO.getTrainBrand());                   // 列车车次类型
                        
                        // 设置列车标签（如果有）
                        // 如果列车有标签（如："高铁","直达"），按逗号分割成数组
                        if (StrUtil.isNotBlank(trainDO.getTrainTag())) {
                            result.setTrainTags(StrUtil.split(trainDO.getTrainTag(), ","));
                        }
                        
                        // 计算到达天数
                        // 计算列车始发时间到该站点出发时间的天数差
                        // 例如：列车10月1日始发，该站点10月2日出发，则返回1（表示第二天到达）
                        long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(trainDO.getDepartureTime(), trainStationRelation.getDepartureTime(), false);
                        result.setDaysArrived((int) betweenDay);
                        
                        // 设置售票状态
                        // 如果当前时间晚于开售时间，则状态为0（可售），否则为1（未开售）
                        result.setSaleStatus(new Date().after(trainDO.getSaleTime()) ? 0 : 1);
                        
                        // 设置开售时间：将Date转换为"MM-dd HH:mm"格式（如：12-25 08:00）
                        result.setSaleTime(convertDateToLocalTime(trainDO.getSaleTime(), "MM-dd HH:mm"));
                        
                        // 将构建好的车票信息添加到结果列表
                        ticketResult.add(result);
                        
                        // 将车票信息存入缓存Map，Key格式：车次ID_出发站_到达站
                        // Value：车票信息的JSON字符串
                        // 这样后续查询相同路线时可以直接从缓存获取，无需访问数据库
                        regionTrainStationAllMap.put(
                                CacheUtil.buildKey(String.valueOf(trainStationRelation.getTrainId()), 
                                                  trainStationRelation.getDeparture(), 
                                                  trainStationRelation.getArrival()), 
                                JSON.toJSONString(result)
                        );
                    }
                    
                    // 将查询结果批量写入Redis缓存
                    // 使用putAll方法一次性将所有车票信息写入Redis Hash
                    // 这样后续查询相同地区对的车票时，可以直接从缓存获取，大幅提升查询性能
                    stringRedisTemplate.opsForHash().putAll(buildRegionTrainStationHashKey, regionTrainStationAllMap);
                }
            } finally {
                // 无论成功与否，都要释放锁，避免死锁
                lock.unlock();
            }

            // 处理缓存结果
            // 如果ticketResult为空（说明是从缓存读取的），需要从regionTrainStationAllMap中解析JSON字符串
            // 如果ticketResult不为空（说明是从数据库查询的），直接使用已有的结果
            ticketResult = CollUtil.isEmpty(ticketResult)
                    ? regionTrainStationAllMap.values().stream()
                    .map(
                            each -> JSON.parseObject(each.toString(), TicketListDTO.class)
                    )
                    .collect(Collectors.toList())
                    : ticketResult;

            // 按出发时间排序
            // 使用TimeStringComparator对车票列表按出发时间从早到晚排序
            // 这样用户可以看到最早出发的车次排在前面
            ticketResult = ticketResult.stream()
                    .sorted(new TimeStringComparator())
                    .collect(Collectors.toList());

            // 为每个车次补充座位价格和余票信息
            // 遍历所有车票，为每个车次查询座位类型、价格和余票数量
            for (TicketListDTO each : ticketResult) {
                // 从缓存或数据库获取该路线的座位价格信息
                // 缓存Key格式：TRAIN_STATION_PRICE + 车次ID_出发站_到达站
                String trainStationPriceStr = distributedCache.safeGet(
                        String.format(TRAIN_STATION_PRICE, each.getTrainId(), each.getDeparture(), each.getArrival()),
                        String.class,
                        () -> {
                            // 如果缓存不存在，从数据库查询座位价格信息
                            LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                                    .eq(TrainStationPriceDO::getTrainId, each.getTrainId())
                                    .eq(TrainStationPriceDO::getDeparture, each.getDeparture())
                                    .eq(TrainStationPriceDO::getArrival, each.getArrival());
                            /**
                             * 需要 JSON 转换的原因：
                             * Redis 只能存储字符串，对象必须序列化为 JSON
                             */
                            return JSON.toJSONString(trainStationPriceMapper.selectList(trainStationPriceWrapper));
                        },
                        ADVANCE_TICKET_DAY,
                        TimeUnit.DAYS
                );
                
                // 将JSON字符串解析为座位价格对象列表
                // 包含该路线所有座位类型的价格信息（如：商务座价格、一等座价格等）
                List<TrainStationPriceDO> trainStationPriceDOList = JSON.parseArray(trainStationPriceStr, TrainStationPriceDO.class);
                
                // 初始化座位类型列表，用于存储每个座位类型的详细信息
                List<SeatClassDTO> seatClassList = new ArrayList<>();
                
                // 遍历每种座位类型的价格信息，补充余票数量
                trainStationPriceDOList.forEach(item -> {
                    // 获取座位类型编码（字符串格式）
                    String seatType = String.valueOf(item.getSeatType());
                    
                    // 构建余票缓存的Key后缀：车次ID_出发站_到达站
                    // 注意：这里使用的是item的出发站和到达站，可能与each的不同（因为可能有多个路线段）
                    String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                    
                    // 从Redis Hash中获取该路线该座位类型的余票数量
                    // Key: TRAIN_STATION_REMAINING_TICKET + keySuffix
                    // Field: seatType（座位类型编码）
                    Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
                    
                    // 处理余票数量：如果缓存中有值则解析，如果没有则从数据库加载
                    int quantity = Optional.ofNullable(quantityObj)
                            // 将Object转为String
                            .map(Object::toString)
                            // 将String转为Integer
                            .map(Integer::parseInt)
                            // 如果quantityObj为null（缓存未命中），则从数据库加载
                            .orElseGet(() -> {
                                // 调用缓存加载器，从数据库查询并加载到缓存
                                // 这会查询该车次所有路线段的余票信息并写入Redis
                                Map<String, String> seatMarginMap = seatMarginCacheLoader.load(
                                        String.valueOf(each.getTrainId()), 
                                        seatType, 
                                        item.getDeparture(), 
                                        item.getArrival()
                                );
                                // 从加载的结果中获取该座位类型的余票数量
                                // 如果还是没有，返回0（表示没有余票）
                                return Optional.ofNullable(seatMarginMap.get(String.valueOf(item.getSeatType())))
                                        .map(Integer::parseInt)
                                        .orElse(0);
                            });
                    
                    // 构建座位类型DTO对象
                    // 参数说明：
                    // - item.getSeatType(): 座位类型编码（如：0=商务座）
                    // - quantity: 余票数量
                    // - new BigDecimal(item.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP): 
                    //   价格转换：数据库存储的是分（如：1000分），转换为元（10.0元），保留1位小数，四舍五入
                    // - false: 是否售罄（这里暂时设为false，实际应该根据quantity判断）
                    seatClassList.add(new SeatClassDTO(
                            item.getSeatType(), 
                            quantity, 
                            new BigDecimal(item.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP), 
                            false
                    ));
                });
                
                // 将座位类型列表设置到车票DTO中
                // 这样前端就可以显示每种座位类型的价格和余票信息
                each.setSeatClassList(seatClassList);
            }
        }
        
        // 构建并返回查询结果
        // 将查询到的车票列表封装为响应对象返回给前端
        return TicketPageQueryRespDTO.builder()
                .ticketList(ticketResult)
                .departureStationList(buildDepartureStationList(ticketResult))
                .arrivalStationList(buildArrivalStationList(ticketResult))
                .trainBrandList(buildTrainBrandList(ticketResult))
                .seatClassTypeList(buildSeatClassList(ticketResult))
                .build();
    }

    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV2(TicketPageQueryReqDTO requestParam) {
        return null;
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV1(PurchaseTicketReqDTO requestParam) {
        return null;
    }

    @Override
    public TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam) {
        return null;
    }

    @Override
    public TicketPurchaseRespDTO executePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        return null;
    }

    @Override
    public PayInfoRespDTO getPayInfo(String orderSn) {
        return null;
    }

    @Override
    public void cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {

    }

    @Override
    public RefundTicketRespDTO commonTicketRefund(RefundTicketReqDTO requestParam) {
        return null;
    }

    /**
     * 构建出发站列表（去重）
     *
     * <p>
     * 该方法从车票结果列表中提取所有不重复的出发站编码，用于前端展示出发站筛选选项。
     * <p>
     * 处理流程：
     * 1. 从车票列表中提取每个车票的出发站编码
     * 2. 使用 distinct() 去重，确保每个出发站只出现一次
     * 3. 收集为列表返回
     * <p>
     *
     * @param ticketResults 车票结果列表，不能为null
     * @return 不重复的出发站编码列表，按出现顺序排列
     */
    private List<String> buildDepartureStationList(List<TicketListDTO> ticketResults) {
        // 处理车票列表
        return ticketResults.stream()
                // 提取每个车票的出发站编码
                // map(TicketListDTO::getDeparture) 等价于 map(ticket -> ticket.getDeparture())
                .map(TicketListDTO::getDeparture)
                // 去重：确保每个出发站编码只出现一次
                // distinct() 使用 equals() 方法判断是否重复
                .distinct()
                // 收集为List集合
                .collect(Collectors.toList());
    }

    /**
     * 构建到达站列表（去重）
     *
     * <p>
     * 该方法从车票结果列表中提取所有不重复的到达站编码，用于前端展示到达站筛选选项。
     * <p>
     * 处理流程：
     * 1. 从车票列表中提取每个车票的到达站编码
     * 2. 使用 distinct() 去重，确保每个到达站只出现一次
     * 3. 收集为列表返回
     * <p>
     *
     * @param ticketResults 车票结果列表，不能为null
     * @return 不重复的到达站编码列表，按出现顺序排列
     */
    private List<String> buildArrivalStationList(List<TicketListDTO> ticketResults) {
        // 处理车票列表
        return ticketResults.stream()
                // 提取每个车票的到达站编码
                // map(TicketListDTO::getArrival) 等价于 map(ticket -> ticket.getArrival())
                .map(TicketListDTO::getArrival)
                // 去重：确保每个到达站编码只出现一次
                // distinct() 使用 equals() 方法判断是否重复
                .distinct()
                // 收集为List集合
                .collect(Collectors.toList());
    }

    /**
     * 构建座位类型列表（去重）
     *
     * <p>
     * 该方法从车票结果列表中提取所有不重复的座位类型编码，用于前端展示座位类型筛选选项。
     * <p>
     * 处理流程：
     * 1. 遍历所有车票，获取每个车票的座位类型列表
     * 2. 提取每个座位类型的编码，使用HashSet自动去重
     * 3. 将HashSet转换为List返回
     * <p>
     *
     * 示例：
     * <pre>
     * 输入：[
     *   {seatClassList: [{type: 0}, {type: 1}]},  // 商务座、一等座
     *   {seatClassList: [{type: 1}, {type: 2}]},  // 一等座、二等座
     *   {seatClassList: [{type: 0}]}              // 商务座
     * ]
     * 输出：[0, 1, 2]  // 去重后的座位类型列表（商务座、一等座、二等座）
     * </pre>
     *
     * @param ticketResults 车票结果列表，不能为null
     * @return 不重复的座位类型编码列表（如：[0, 1, 2] 表示商务座、一等座、二等座）
     */
    private List<Integer> buildSeatClassList(List<TicketListDTO> ticketResults) {
        // 使用HashSet自动去重，确保每个座位类型编码只出现一次
        // HashSet的add()方法会自动判断是否已存在，避免重复添加
        Set<Integer> resultSeatClassList = new HashSet<>();
        
        // 遍历所有车票
        for (TicketListDTO each : ticketResults) {
            // 遍历每个车票的座位类型列表
            // 每个车票可能包含多种座位类型（如：商务座、一等座、二等座）
            for (SeatClassDTO item : each.getSeatClassList()) {
                // 将座位类型编码添加到Set中
                // HashSet会自动去重，如果已存在则不会重复添加
                resultSeatClassList.add(item.getType());
            }
        }
        
        // 将HashSet转换为List返回
        // 使用Stream API将Set转为List，保持代码风格一致
        return resultSeatClassList.stream()
                .collect(Collectors.toList());
    }

    /**
     * 构建列车品牌列表（去重）
     *
     * <p>
     * 该方法从车票结果列表中提取所有不重复的列车品牌编码，用于前端展示列车品牌筛选选项。
     * <p>
     * 处理流程：
     * 1. 遍历所有车票，获取每个车票的列车品牌信息
     * 2. 如果列车品牌不为空，按逗号分割字符串（因为一个车次可能有多个品牌标识）
     * 3. 将字符串转换为整数编码，使用HashSet自动去重
     * 4. 将HashSet转换为List返回
     * <p>
     * 示例：
     * <pre>
     * 输入：[
     *   {trainBrand: "1,2"},  // G字头、D字头
     *   {trainBrand: "2,3"},  // D字头、C字头
     *   {trainBrand: "1"}      // G字头
     * ]
     * 输出：[1, 2, 3]  // 去重后的列车品牌列表（G字头、D字头、C字头）
     * </pre>
     * <p>
     * 注意事项：
     * - trainBrand字段可能是逗号分隔的字符串（如："1,2,3"），需要分割处理
     * - 如果trainBrand为空或null，则跳过该车票
     *
     * @param ticketResults 车票结果列表，不能为null
     * @return 不重复的列车品牌编码列表（如：[1, 2, 3] 表示G字头、D字头、C字头）
     */
    private List<Integer> buildTrainBrandList(List<TicketListDTO> ticketResults) {
        // 使用HashSet自动去重，确保每个列车品牌编码只出现一次
        Set<Integer> trainBrandSet = new HashSet<>();
        
        // 遍历所有车票
        for (TicketListDTO each : ticketResults) {
            // 检查列车品牌字段是否不为空
            // trainBrand可能是逗号分隔的字符串（如："1,2,3"），表示该车次有多个品牌标识
            if (StrUtil.isNotBlank(each.getTrainBrand())) {
                // 按逗号分割字符串，转换为整数列表，并添加到HashSet中
                // 例如："1,2,3" -> ["1", "2", "3"] -> [1, 2, 3] -> 添加到HashSet
                trainBrandSet.addAll(
                        StrUtil.split(each.getTrainBrand(), ",")  // 按逗号分割，得到字符串数组
                                .stream()
                                .map(Integer::parseInt)            // 将每个字符串转换为整数
                                .collect(Collectors.toList())      // 收集为List
                );
                // HashSet的addAll()方法会自动去重，如果品牌编码已存在则不会重复添加
            }
        }
        
        // 将HashSet转换为List返回
        // 使用Java 9+的stream().toList()方法（不可变List）
        return trainBrandSet.stream().toList();
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
