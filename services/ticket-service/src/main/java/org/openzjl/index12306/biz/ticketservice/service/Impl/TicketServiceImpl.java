package org.openzjl.index12306.biz.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.common.enums.SourceEnum;
import org.openzjl.index12306.biz.ticketservice.common.enums.TicketStatusEnum;
import org.openzjl.index12306.biz.ticketservice.dao.entity.*;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.*;
import org.openzjl.index12306.biz.ticketservice.dto.domain.SeatClassDTO;
import org.openzjl.index12306.biz.ticketservice.dto.domain.SeatTypeCountDTO;
import org.openzjl.index12306.biz.ticketservice.dto.domain.TicketListDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.*;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.ticketservice.common.enums.TicketChainMarkEnum;
import org.openzjl.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import org.openzjl.index12306.biz.ticketservice.remote.PayRemoteService;
import org.openzjl.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.RefundTicketRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import org.openzjl.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderCreateRemoteReqDTO;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderItemCreateRemoteReqDTO;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.TicketService;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TokenResultDTO;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.select.TrainSeatTypeSelector;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import org.openzjl.index12306.biz.ticketservice.toolkit.DateUtil;
import org.openzjl.index12306.biz.ticketservice.toolkit.TimeStringComparator;
import org.openzjl.index12306.framework.starter.bases.ApplicationContextHolder;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.cache.toolkit.CacheUtil;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.openzjl.index12306.framework.starter.log.annotation.ILog;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
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
    private final Environment environment;
    private final TrainSeatTypeSelector trainSeatTypeSelector;
    private final PayRemoteService payRemoteService;
    private TicketService ticketService;

    /**
     * 令牌桶为空时的异步刷新线程池
     * <p>
     * 用途：当令牌桶为空（无票可售）时，异步执行令牌桶刷新任务，从数据库重新加载最新余票并更新令牌桶。
     * </p>
     *
     * <p>为什么使用单线程？</p>
     * <ul>
     *     <li>串行执行刷新任务，避免并发刷新导致的数据不一致。</li>
     *     <li>同一车次的刷新任务按顺序执行，确保令牌桶更新的准确性。</li>
     *     <li>减少数据库压力，避免多个线程同时查询同一车次的余票。</li>
     *     <li>单线程足够处理刷新任务，刷新操作本身不耗时（主要是数据库查询）。</li>
     * </ul>
     *
     * <p>使用场景：</p>
     * <ul>
     *     <li>延迟刷新：当令牌桶为空时，不立即刷新，而是延迟一段时间后异步刷新。</li>
     *     <li>定时刷新：定期检查并刷新令牌桶，确保数据实时性。</li>
     *     <li>批量刷新：将多个车次的刷新任务放入队列，统一处理。</li>
     * </ul>
     */
    private final ScheduledExecutorService tokenIsNullRefreshExecutor = Executors.newScheduledThreadPool(1);

    @Value("${ticket.availability.cache-update.type}")
    private String ticketAvailabilityCacheUpdateType;

    @Value("${framework.cache.redis.prefix}")
    private String cacheRedisPrefix;

    /**
     * 本地锁缓存（JVM内锁）
     * <p>
     * 用途：用于在单个JVM实例内进行同步控制，避免使用分布式锁时的网络开销
     * <p>
     * 工作原理：
     * - Key: 锁的唯一标识（如：车次ID_出发站_到达站）
     * - Value: ReentrantLock对象，用于JVM内的线程同步
     * - 过期时间：1天（写入后1天自动清理，防止内存泄漏）
     * <p>
     * 使用场景：
     * - 当需要在单个服务实例内进行同步控制时（如：刷新某个车次的余票缓存）
     * - 避免多个线程同时刷新同一个车次的缓存，造成重复查询数据库
     * - 相比分布式锁，本地锁性能更好（无网络开销），但只能控制单个JVM内的线程
     * <p>
     * 注意事项：
     * - 这是JVM级别的锁，不能跨服务实例同步
     * - 如果需要跨服务实例同步，应使用分布式锁（Redisson）
     * - 过期时间设置为1天，确保长时间不使用的锁能够自动清理
     */
    private final Cache<String, ReentrantLock> localLockMap = Caffeine.newBuilder()
            // 写入后1天过期，自动清理不使用的锁对象，防止内存泄漏
            .expireAfterWrite(1, TimeUnit.DAYS)
            .build();

    /**
     * Token车票刷新缓存
     * <p>
     * 用途：防止短时间内重复刷新token或车票信息，实现防抖功能
     * <p>
     * 工作原理：
     * - Key: 刷新操作的唯一标识（如：用户ID_车次ID）
     * - Value: 刷新标记或结果对象
     * - 过期时间：1分钟（写入后1分钟自动清理）
     * <p>
     * 使用场景：
     * - 防止用户频繁点击刷新按钮，导致短时间内多次刷新
     * - 在刷新操作进行中时，如果再次触发刷新，直接返回缓存的结果
     * - 实现"防抖"功能：1分钟内只允许刷新一次
     * <p>
     * 性能优化：
     * - 使用Caffeine本地缓存，读写性能极高（纳秒级）
     * - 相比Redis，本地缓存无网络开销，响应更快
     * - 过期时间短（1分钟），内存占用小
     * <p>
     * 注意事项：
     * - 这是JVM级别的缓存，不同服务实例之间不共享
     * - 如果需要跨服务实例共享，应使用Redis缓存
     * - 过期时间设置为1分钟，平衡了防抖效果和实时性
     */
    private final Cache<String, Object> tokenTicketsRefreshMap = Caffeine.newBuilder()
            // 写入后1分钟过期，实现防抖功能：1分钟内只允许刷新一次
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

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

    /**
     * 车票查询V2版本（优化版）
     * <p>
     * ========== V2版本与V1版本的核心差异 ==========
     * <p>
     * 【V1版本的问题】：
     * 1. 逐个查询座位价格：使用distributedCache.safeGet()，每个车次路线都要单独执行一次Redis GET命令
     *    - 如果有100个车次路线，就需要100次网络往返（RTT）
     *    - 每次网络往返耗时约1-5ms，100次就是100-500ms
     * <p>
     * 2. 逐个查询余票：在forEach循环中，每个座位类型都要单独执行一次HGET命令
     *    - 如果有100个车次路线，每个路线3种座位类型，就需要300次网络往返
     *    - 300次网络往返耗时约300-1500ms
     * <p>
     * 3. 双重检查锁定：需要获取分布式锁，检查缓存，可能查询数据库
     *    - 锁竞争导致线程等待
     *    - 数据库查询耗时更长（10-100ms）
     * <p>
     * 【V2版本的优化】：
     * 1. 批量查询座位价格：使用Redis Pipeline一次性批量查询所有车次路线的座位价格
     *    - 100个车次路线只需要1次网络往返
     *    - 性能提升：100倍（从100次减少到1次）
     * <p>
     * 2. 批量查询余票：使用Redis Pipeline一次性批量查询所有座位类型的余票
     *    - 300个座位类型只需要1次网络往返
     *    - 性能提升：300倍（从300次减少到1次）
     * <p>
     * 3. 假设缓存已存在：直接从缓存读取，无需双重检查锁定和数据库查询
     *    - 避免锁竞争
     *    - 避免数据库查询延迟
     * <p>
     * 【性能提升计算】：
     * 假设查询100个车次路线，每个路线3种座位类型：
     * - V1版本：100次GET + 300次HGET = 400次网络往返 ≈ 400-2000ms
     * - V2版本：1次Pipeline(GET) + 1次Pipeline(HGET) = 2次网络往返 ≈ 2-10ms
     * - 性能提升：200-1000倍
     * <p>
     * ========== 数据获取顺序说明 ==========
     * <p>
     * 【第一步】责任链处理
     *   - 执行参数校验、权限校验等前置处理
     * <p>
     * 【第二步】获取站点地区信息
     *   - 从Redis Hash (REGION_TRAIN_STATION_MAPPING) 获取出发站和到达站所属的地区
     *   - 目的：确定查询的地区范围（如：华北 -> 华东）
     * <p>
     * 【第三步】获取车次路线信息列表
     *   - 构建地区列车站点缓存的Redis Hash Key（格式：REGION_TRAIN_STATION + 出发地区 + 到达地区）
     *   - 从Redis Hash中获取该地区对的所有车次路线信息（车次ID、出发站、到达站、出发时间等）
     *   - 解析JSON字符串为TicketListDTO对象列表，并按出发时间排序
     *   - 此时得到：车次路线列表（但还没有座位价格和余票信息）
     *   - 注意：这里存储的是"车次路线信息"，不是用户购买的"车票"
     * <p>
     * 【第四步】批量获取座位价格信息
     *   - 为每个车次路线构建座位价格缓存的Redis Key（格式：TRAIN_STATION_PRICE + 车次ID_出发站_到达站）
     *   - 使用Redis Pipeline批量执行GET命令，获取所有车次路线的座位价格数据（JSON字符串）
     *   - 此时得到：每个车次路线的座位价格列表（商务座、一等座、二等座的价格）
     * <p>
     * 【第五步】批量获取余票数量
     *   - 解析座位价格数据，为每个座位类型构建余票缓存的Redis Hash Key
     *   - 使用Redis Pipeline批量执行HGET命令，获取每个座位类型的余票数量
     *   - 此时得到：每个座位类型的余票数量
     * <p>
     * 最终数据组装：
     *   - 将车次路线信息 + 座位价格 + 余票数量 组装成完整的响应对象返回给前端
     * <p>
     * 性能优化点：
     *   1. 使用Redis Pipeline批量查询，减少网络往返次数（核心优化）
     *   2. 直接从缓存读取，避免数据库查询
     *   3. 数据按地区分组缓存，提高查询效率
     *   4. 避免分布式锁竞争，提升并发性能
     *
     * @param requestParam 车票查询请求参数（包含出发站、到达站、出发日期等）
     * @return 车票查询响应对象（包含车票列表、筛选选项等）
     */
    @Override
    public TicketPageQueryRespDTO pageListTicketQueryV2(TicketPageQueryReqDTO requestParam) {
        // 执行责任链处理
        // 触发车票查询责任链，执行一系列前置处理（如参数校验、权限校验等）
        ticketPageQueryAbstractChainContext.handler(
                TicketChainMarkEnum.TRAIN_QUERY_FILTER.name(), requestParam
        );
        
        // 获取Redis操作模板
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        
        // 获取出发站和到达站所属的地区
        List<Object> stationDetails = stringRedisTemplate.opsForHash()
                .multiGet(REGION_TRAIN_STATION_MAPPING, Lists.newArrayList(requestParam.getFromStation(), requestParam.getToStation()));
        
        // 构建地区列车站点缓存的Redis Hash Key
        // 格式：REGION_TRAIN_STATION + 出发地区 + 到达地区
        // 例如：index12306-ticket-service:region_train_station:华北:华东
        // stationDetails.get(0) 是出发站所在地区，stationDetails.get(1) 是到达站所在地区
        String buildRegionTrainStationHashKey = String.format(REGION_TRAIN_STATION, stationDetails.get(0), stationDetails.get(1));
        
        // 从Redis Hash中获取该地区对的所有车次路线信息
        // entries() 方法获取Hash中的所有字段和值
        // Hash Field: 车次ID_出发站_到达站（如：G123_1001_2001）
        // Hash Value: 车次路线信息的JSON字符串（包含车次ID、出发站、到达站、出发时间等）
        Map<Object, Object> regionTrainStationAllMap = stringRedisTemplate.opsForHash().entries(buildRegionTrainStationHashKey);

        // 将缓存中的JSON字符串解析为TicketListDTO对象列表（车次路线信息）
        // 然后按出发时间从早到晚排序
        // 注意：这里存储的是"车次路线信息"，不是用户购买的"车票"
        // 每个条目代表一个车次在特定出发站和到达站之间的运行信息
        List<TicketListDTO> trainRouteResults = regionTrainStationAllMap.values().stream()
                // 将每个JSON字符串解析为TicketListDTO对象
                // each是Object类型（实际是JSON字符串），需要先转为String再解析
                .map(each -> JSON.parseObject(each.toString(), TicketListDTO.class))
                .sorted(new TimeStringComparator())
                // 收集为List集合
                .collect(Collectors.toList());

        // 构建所有车次路线的座位价格缓存Key列表
        // 作用：为每个车次路线构建对应的Redis缓存Key，用于后续批量查询座位价格信息
        // 
        // 背景说明：
        // - trainRouteResults 是查询到的车次路线列表，每个路线包含：车次ID、出发站、到达站等信息
        // - 但是车次路线的座位价格信息（商务座多少钱、一等座多少钱等）存储在Redis中
        // - Redis中每个车次路线的座位价格用一个Key存储，格式为：车次ID_出发站_到达站
        // 
        // 举例说明：
        // 假设查询到3个车次路线：
        //   路线1：G123次，北京(1001) -> 上海(2001)
        //   路线2：G456次，北京(1001) -> 广州(3001)
        //   路线3：D789次，上海(2001) -> 杭州(4001)
        // 
        // 这段代码会构建3个Redis Key：
        //   Key1: index12306-ticket-service:train_station_price:G123_1001_2001
        //   Key2: index12306-ticket-service:train_station_price:G456_1001_3001
        //   Key3: index12306-ticket-service:train_station_price:D789_2001_4001
        // 
        // 这些Key用于后续从Redis中批量查询每个车次路线的座位价格信息
        List<String> trainStationPriceKeys = trainRouteResults.stream()
                // 遍历每个车次路线，为每个路线构建一个Redis Key
                // String.format() 将车次ID、出发站、到达站填充到TRAIN_STATION_PRICE模板中
                // 
                // 模板格式：TRAIN_STATION_PRICE = "index12306-ticket-service:train_station_price:%s_%s_%s"
                // 填充示例：
                //   - 车次ID: "G123"
                //   - 出发站: "1001"
                //   - 到达站: "2001"
                //   - 结果: "index12306-ticket-service:train_station_price:G123_1001_2001"
                .map(each -> String.format(cacheRedisPrefix + TRAIN_STATION_PRICE, each.getTrainId(), each.getDeparture(), each.getArrival()))
                // 收集为List集合，得到所有车票的Redis Key列表
                .collect(Collectors.toList());


        // 使用Pipeline技术批量执行Redis GET命令，大幅提升性能
        // Pipeline原理：将多个命令打包发送给Redis，减少网络往返次数（RTT）
        // 性能提升：如果有100个Key需要查询，使用Pipeline可以将100次网络往返减少到1次
        // 注意：Pipeline中的命令是原子执行的，但Pipeline本身不是事务，中间某个命令失败不会回滚
        // 
        // 查询结果说明：
        // - trainStationPriceObjs 是一个 List<Object>，每个元素对应一个车次路线的座位价格数据
        // - 包含该车次路线所有座位类型的价格信息（商务座、一等座、二等座等）
        List<Object> trainStationPriceObjs = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {

            // connection.stringCommands().get() 执行Redis GET命令
            // getBytes() 将字符串Key转换为字节数组（Redis底层使用字节数组存储）
            // 为每个车次路线的座位价格缓存Key执行GET命令
            trainStationPriceKeys.forEach(each -> connection.stringCommands().get(each.getBytes()));
            // RedisCallback必须返回一个值，这里返回null即可, 实际返回的数据在executePipelined()的返回值中
            return null;
        });

        // 解析座位价格数据并构建余票缓存Key列表
        // 存储所有座位价格对象
        // 例如：如果有3个车票，每个车票有3种座位类型，则这个列表包含9个TrainStationPriceDO对象
        List<TrainStationPriceDO> trainStationPriceList = new ArrayList<>();
        
        // 存储所有余票缓存的Redis Hash Key列表
        // 每个Key对应一个车次+出发站+到达站的组合，用于查询该路段的余票信息
        // Key格式：cacheRedisPrefix + TRAIN_STATION_REMAINING_TICKET + 车次ID_出发站_到达站
        // 例如：index12306-ticket-service:train_station_remaining_ticket:G123_1001_2001
        List<String> trainStationRemainingKeyList = new ArrayList<>();
        

        // 遍历返回的座位价格数据, each是包含一个车票的所有座位类型价格信息
        for (Object each : trainStationPriceObjs) {
            // 将JSON数组字符串解析为TrainStationPriceDO对象列表
            // 例如：JSON字符串 "[{trainId:'G123',seatType:0,price:1000,...}, {trainId:'G123',seatType:1,price:500,...}]" 解析为包含2个TrainStationPriceDO对象的列表
            List<TrainStationPriceDO> trainStationPriceDOList = JSON.parseArray(each.toString(), TrainStationPriceDO.class);
            
            // 将所有座位价格对象添加到总列表中
            // 这样可以将多个车票的座位价格合并到一个列表中，方便后续批量处理
            trainStationPriceList.addAll(trainStationPriceDOList);
            
            // 为每个座位价格对象构建对应的余票缓存Key
            for (TrainStationPriceDO item : trainStationPriceDOList) {
                // 格式：cacheRedisPrefix + TRAIN_STATION_REMAINING_TICKET + 车次ID_出发站_到达站
                // 例如：index12306-ticket-service:train_station_remaining_ticket:G123_1001_2001
                String trainStationRemainingKey = cacheRedisPrefix + TRAIN_STATION_REMAINING_TICKET + StrUtil.join("_", item.getTrainId(), item.getDeparture(), item.getArrival());
                
                // 将Key添加到列表中
                // 注意：trainStationRemainingKeyList和trainStationPriceList的索引是一一对应的
                // 即 trainStationRemainingKeyList.get(i) 对应的余票Key
                // 用于查询 trainStationPriceList.get(i) 这个座位价格对象对应的余票数量
                trainStationRemainingKeyList.add(trainStationRemainingKey);
            }
        }
        
        // 批量获取余票数量
        // 结构层次：
        // - Hash Key（外层Key）：标识一个车次路线的余票信息
        //   格式：TRAIN_STATION_REMAINING_TICKET + 车次ID_出发站_到达站
        //   例如："index12306-ticket-service:train_station_remaining_ticket:G123_1001_2001"
        //   含义：G123次列车从北京(1001)到上海(2001)的余票信息
        // 
        // - Hash Field（内层字段名）：座位类型编码
        //   例如："0"（商务座）、"1"（一等座）、"2"（二等座）
        // 
        // - Hash Value（内层字段值）：该座位类型的余票数量
        //   例如："10"（表示还有10张票）
        // 
        // 实际存储示例：
        // Hash Key: "index12306-ticket-service:train_station_remaining_ticket:G123_1001_2001"
        //   ├─ Field: "0" → Value: "10"  (商务座还有10张)
        //   ├─ Field: "1" → Value: "25"  (一等座还有25张)
        //   └─ Field: "2" → Value: "50"  (二等座还有50张)
        List<Object> trainStationRemainingObjs = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            // 遍历所有Key和座位价格对象，为每个组合执行HGET命令
            for (int i = 0; i < trainStationRemainingKeyList.size(); i++) {
                // 执行Redis HGET命令，从Hash中获取指定座位类型的余票数量
                // 参数1：Hash Key（转换为字节数组）
                // 参数2：Hash Field（座位类型编码，转换为字节数组）
                // 例如：HGET "index12306-ticket-service:train_station_remaining_ticket:G123_1001_2001" "0"
                // 返回：该路段商务座的余票数量（如："10"）
                connection.hashCommands().hGet(
                        trainStationRemainingKeyList.get(i).getBytes(),  // Hash Key
                        trainStationPriceList.get(i).getSeatType().toString().getBytes()  // Hash Field（座位类型）
                );
            }
            // RedisCallback必须返回一个值，这里返回null即可, 实际返回的数据在executePipelined()的返回值中
            return null;
        });

        // 为每个车次路线组装座位价格和余票信息
        // 目的：将扁平化的座位价格和余票数据按照车次路线重新分组
        // 
        // 数据组织说明：
        // - trainRouteResults: 车次路线列表，例如：[路线1(G123), 路线2(G456), 路线3(D789)]
        // - trainStationPriceList: 扁平化的座位价格列表，按路线顺序排列
        //   例如：[路线1的座位1, 路线1的座位2, 路线1的座位3, 路线2的座位1, 路线2的座位2, ...]
        // - trainStationRemainingObjs: 扁平化的余票列表，与trainStationPriceList一一对应
        //   例如：[路线1座位1余票, 路线1座位2余票, 路线1座位3余票, 路线2座位1余票, ...]
        for (TicketListDTO each : trainRouteResults) {
            // 根据车次类型获取该车次支持的座位类型列表
            // 例如：高铁(G)可能有[0,1,2]（商务座、一等座、二等座），动车(D)可能有[1,2]（一等座、二等座）
            List<Integer> seatTypesByCode = VehicleTypeEnum.findSeatTypeByCode(each.getTrainType());
            
            // 从扁平化的列表中提取当前车次路线的余票数据
            // subList(currentIndex, currentIndex + seatTypesByCode.size()) 提取当前路线的所有座位类型余票
            // 例如：如果当前路线有3种座位类型，且currentIndex=0，则提取索引[0,1,2]的数据
            List<Object> remainingTicket = new ArrayList<>(trainStationRemainingObjs.subList(0, seatTypesByCode.size()));
            
            // 从扁平化的列表中提取当前车次路线的座位价格数据
            // 与余票数据一一对应，索引范围相同
            List<TrainStationPriceDO> trainStationPriceDOSub = new ArrayList<>(trainStationPriceList.subList(0, seatTypesByCode.size()));

            // 构建座位类型详细信息列表
            // 将座位价格和余票信息组装成SeatClassDTO对象，用于前端展示
            List<SeatClassDTO> seatClassList = new ArrayList<>();
            
            // 遍历当前车次路线的所有座位类型，为每个座位类型构建详细信息
            for (int i = 0; i < trainStationPriceDOSub.size(); i++) {
                // 获取当前座位类型的价格信息
                TrainStationPriceDO trainStationPriceDO = trainStationPriceDOSub.get(i);
                
                // 构建座位类型DTO对象，包含座位类型、余票数量、价格等信息
                SeatClassDTO seatClassDTO = SeatClassDTO.builder()
                        // 座位类型编码（如：0=商务座，1=一等座，2=二等座）
                        .type(trainStationPriceDO.getSeatType())
                        // 余票数量：从remainingTicket中获取，转换为整数
                        // remainingTicket.get(i) 是Object类型（实际是String类型的数字），需要先转String再转Integer
                        .quantity(Integer.parseInt(remainingTicket.get(i).toString()))
                        // 座位价格：从分转换为元，保留1位小数，四舍五入
                        // 数据库中价格以"分"为单位存储（如：1000分 = 10.0元），需要除以100转换为元
                        .price(new BigDecimal(trainStationPriceDO.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP))
                        // 是否作为候补选项（默认false，表示不是候补）
                        .candidate(false)
                        .build();
                
                // 将构建好的座位类型DTO添加到列表中
                seatClassList.add(seatClassDTO);
            }
            
            // 将座位类型列表设置到车次路线对象中
            // 这样每个车次路线就包含了完整的座位价格和余票信息
            each.setSeatClassList(seatClassList);
        }
        
        // 构建并返回响应对象
        // 将查询结果封装成响应对象，返回给前端
        return TicketPageQueryRespDTO.builder()
                // 车次路线列表（已包含座位价格和余票信息）
                .ticketList(trainRouteResults)
                // 出发站列表（去重后的出发站编码列表，用于前端筛选）
                .departureStationList(buildDepartureStationList(trainRouteResults))
                // 到达站列表（去重后的到达站编码列表，用于前端筛选）
                .arrivalStationList(buildArrivalStationList(trainRouteResults))
                // 列车品牌列表（去重后的列车品牌编码列表，用于前端筛选）
                .trainBrandList(buildTrainBrandList(trainRouteResults))
                // 座位类型列表（去重后的座位类型编码列表，用于前端筛选）
                .seatClassTypeList(buildSeatClassList(trainRouteResults))
                .build();
    }

    @ILog
    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:",
            key = "T(org.openzjl.index12306.framework.starter.bases.ApplicationContextHolder).getBean('environment').getProperty('unique-name','')"
                    + "+'_'+"
                    + "T(org.openzjl.index12306.framework.starter.user.core.UserContext).getUserName()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    @Override
    public TicketPurchaseRespDTO purchaseTicketsV1(PurchaseTicketReqDTO requestParam) {
        /**
         * 责任链模式
         * 验证 1: 参数必填
         * 验证 2: 参数正确性
         * 验证 3: 乘客是否已经购买当前车次
         */
        purchaseTicketAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(), requestParam);
        String lockKey = environment.resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS, requestParam.getTrainId()));
        RLock lock = redissonClient.getLock(lockKey);
        lock.lock();
        try {
            return ticketService.executePurchaseTickets(requestParam);
        } finally {
            lock.unlock();
        }
    }

    @ILog
    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:",
            key = "T(org.openzjl.index12306.framework.starter.bases.ApplicationContextHolder).getBean('environment').getProperty('unique-name','')"
                    + "+'_'+"
                    + "T(org.openzjl.index12306.framework.starter.user.core.UserContext).getUserName()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    /**
     * 购票接口 V2 版本（基于令牌桶限流 + 双重锁机制）。
     * <p>
     * 相比 V1 版本，V2 版本引入了以下优化：
     * </p>
     * <ul>
     *     <li>令牌桶限流：控制购票并发，避免超卖。</li>
     *     <li>按座位类型加锁：不同座位类型的购票操作可以并行，提高并发性能。</li>
     *     <li>本地锁 + 分布式锁：双重锁机制，本地锁减少网络开销，分布式锁保证跨服务实例的同步。</li>
     *     <li>令牌桶刷新机制：当令牌桶为空时，延迟刷新并验证数据准确性。</li>
     * </ul>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>责任链校验：参数校验、权限校验等。</li>
     *     <li>令牌桶限流：从令牌桶获取令牌，控制购票并发。</li>
     *     <li>令牌桶刷新：如果令牌桶为空，触发刷新机制（防抖 + 双重检查锁定）。</li>
     *     <li>按座位类型分组：将乘客按座位类型分组，不同座位类型可以并行处理。</li>
     *     <li>获取锁：为每个座位类型获取本地锁和分布式锁。</li>
     *     <li>执行购票：在锁保护下执行购票核心流程。</li>
     *     <li>释放锁：无论成功与否，都要释放所有锁。</li>
     * </ol>
     *
     * @param requestParam 购票请求参数（包含车次ID、出发站、到达站、乘客信息等）
     * @return 购票响应对象（包含订单号和车票详情列表）
     * @throws ServiceException 当令牌桶为空且已刷新过时抛出
     */
    @Override
    public TicketPurchaseRespDTO purchaseTicketsV2(PurchaseTicketReqDTO requestParam) {
        // 责任链校验
        // 执行购票前置校验（参数校验、权限校验、乘客是否已购票等）
        // 确保请求合法后再继续后续流程
        purchaseTicketAbstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(), requestParam);
        
        // 从令牌桶获取令牌（限流控制）
        // 令牌桶机制：控制购票并发，避免超卖
        // - 如果令牌桶中有令牌，返回令牌，允许购票
        // - 如果令牌桶为空，返回 null，表示当前无票可售
        TokenResultDTO tokenResult = ticketAvailabilityTokenBucket.takeTokenFromBucket(requestParam);
        
        // 令牌桶为空时的处理（刷新机制 + 防抖 + 双重检查锁定）
        // 如果获取不到令牌（令牌桶为空），说明该车次当前无票
        // 此时需要检查是否需要刷新令牌桶（从数据库重新加载余票并更新令牌桶）
        if (tokenResult.getTokenIsNull()) {
            // 第一次检查：快速路径（无锁检查）
            // 检查本地缓存中是否有该车次的刷新标记
            // tokenTicketsRefreshMap 是防抖缓存，1分钟内只允许刷新一次
            // - 如果返回 null：说明最近1分钟内没有刷新过，可能需要触发刷新
            // - 如果返回非 null：说明最近1分钟内已经刷新过，避免重复刷新（防抖）
            Object ifPresentObj = tokenTicketsRefreshMap.getIfPresent(requestParam.getTrainId());
            
            // 如果没有刷新标记，说明可以尝试触发刷新
            if (ifPresentObj == null) {
                // 双重检查锁定（Double-Check Locking）模式
                // 使用 synchronized 确保同一时刻只有一个线程能执行刷新逻辑
                // 锁对象：TicketService.class（类级别的锁，确保所有实例共享同一把锁）
                synchronized (TicketService.class) {
                    // 加锁后再次检查
                    // 获取锁后再次检查缓存，可能其他线程已经写入标记了
                    // 这是双重检查锁定的核心：确保即使多个线程同时通过第一次检查，也只有一个线程执行刷新
                    if (tokenTicketsRefreshMap.getIfPresent(requestParam.getTrainId()) == null) {
                        // 创建刷新标记对象（可以是任意对象，这里使用 new Object()）
                        // 这个对象本身不重要，重要的是它的存在表示"正在刷新或已刷新过"
                        ifPresentObj = new Object();
                        
                        // 写入缓存标记，有效期1分钟
                        // 这样后续1分钟内的请求都会发现已有标记，不会重复刷新
                        tokenTicketsRefreshMap.put(requestParam.getTrainId(), ifPresentObj);
                        
                        // 触发令牌桶刷新任务（延迟10秒执行，异步刷新）
                        // 这个方法会从数据库查询最新余票，并更新令牌桶
                        tokenIsNullRefreshToken(requestParam, tokenResult);
                    }
                    // 如果第二次检查发现已有标记，说明其他线程已经触发刷新，当前线程直接返回
                }
            }
            // 如果第一次检查发现已有标记，说明最近1分钟内已刷新过，并且没有余票，直接报错
            throw new ServiceException("列车站点已无余票");
        }
        
        // 按座位类型分组乘客
        // 初始化锁列表，用于存储所有需要获取的锁
        List<ReentrantLock> localLockList = new ArrayList<>();      // 本地锁列表（JVM内锁）
        List<RLock> distributedLockList = new ArrayList<>();        // 分布式锁列表（跨服务实例锁）
        
        // 将乘客按座位类型分组
        // 例如：3个乘客，2个要商务座(0)，1个要一等座(1)
        // 结果：{0: [乘客1, 乘客2], 1: [乘客3]}
        // 这样不同座位类型的购票操作可以并行处理，提高并发性能
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        
        // 为每个座位类型获取锁
        // 遍历每个座位类型，为每个类型获取本地锁和分布式锁
        seatTypeMap.forEach((seatType, passengerList) -> {
            // 构建锁的key：车次ID_座位类型
            // 例如：LOCK_PURCHASE_TICKETS_V2 = "index12306-ticket-service:lock_purchase-tickets-v2:%s_%s"
            // 结果：index12306-ticket-service:lock_purchase-tickets-v2:G123_0（G123次商务座）
            String lockKey = environment.resolvePlaceholders(String.format(LOCK_PURCHASE_TICKETS_V2, requestParam.getTrainId(), seatType));
            
            // 获取或创建本地锁（JVM内锁）
            // 本地锁用于单个服务实例内的线程同步，性能更好（无网络开销）
            ReentrantLock localLock = localLockMap.getIfPresent(lockKey);
            if (localLock == null) {
                // 双重检查锁定：确保同一时刻只有一个线程创建锁对象
                synchronized (TicketService.class) {
                    // 再次检查，可能其他线程已经创建了
                    if ((localLock = localLockMap.getIfPresent(lockKey)) == null) {
                        // 创建公平锁（fair lock），按请求顺序获取锁，避免线程饥饿
                        // true 表示公平锁，false 表示非公平锁
                        localLock = new ReentrantLock(true);
                        // 存入缓存，1天后自动过期
                        localLockMap.put(lockKey, localLock);
                    }
                }
            }
            // 添加到本地锁列表
            localLockList.add(localLock);
            
            // 获取分布式锁（跨服务实例锁）
            // 使用公平锁（FairLock），按请求顺序获取锁，避免某些服务实例一直获取不到锁
            // 分布式锁用于跨服务实例的同步，确保多个服务实例之间不会同时处理同一车次同一座位类型的购票
            RLock distributedLock = redissonClient.getFairLock(lockKey);
            distributedLockList.add(distributedLock);
        });
        
        // 加锁并执行购票
        try {
            // 先获取所有本地锁（按顺序获取，避免死锁）
            // 本地锁获取速度快（无网络开销），先获取本地锁可以减少等待时间
            localLockList.forEach(ReentrantLock::lock);
            
            // 再获取所有分布式锁（按顺序获取，避免死锁）
            // 分布式锁需要网络通信，耗时较长（1-5ms），但能保证跨服务实例的同步
            distributedLockList.forEach(RLock::lock);
            
            // 在锁保护下执行购票核心流程
            // 此时已经获取了所有必要的锁，可以安全地执行购票操作
            return ticketService.executePurchaseTickets(requestParam);
        } finally {
            // 释放所有锁
            // 无论购票成功与否，都要释放所有锁，避免死锁
            // 使用 try-catch 确保即使释放锁时出现异常，也不会影响其他锁的释放
            
            // 释放所有本地锁（按顺序释放）
            localLockList.forEach(localLock -> {
                try {
                    localLock.unlock();
                } catch (Throwable ignored) {
                    // 忽略释放锁时的异常，确保其他锁能正常释放
                }
            });

            // 释放所有分布式锁（按顺序释放）
            distributedLockList.forEach(distributedLock -> {
                try {
                    distributedLock.unlock();
                } catch (Throwable ignored) {
                    // 忽略释放锁时的异常，确保其他锁能正常释放
                }
            });
        }
    }

    /**
     * 执行购票核心流程
     * <p>
     * 该方法在分布式锁保护下执行，确保同一车次同一用户的购票操作串行化，避免并发冲突。
     * 使用 {@code @Transactional} 注解确保数据一致性，任何异常都会触发事务回滚。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>获取车次信息：从缓存或数据库获取车次基本信息。</li>
     *     <li>选择座位：根据车次类型和乘客信息，选择合适的座位（车厢号、座位号）。</li>
     *     <li>保存车票记录：将选中的座位信息保存到车票表，状态为"未支付"。</li>
     *     <li>构建订单项：为每个乘客构建订单项信息（包含价格、座位、乘客信息等）。</li>
     *     <li>查询站点关系：获取出发站和到达站的时间信息。</li>
     *     <li>创建订单：调用订单服务创建订单，返回订单号。</li>
     *     <li>返回结果：封装订单号和车票详情返回给前端。</li>
     * </ol>
     *
     * <p>注意事项：</p>
     * <ul>
     *     <li>该方法在事务中执行，任何步骤失败都会回滚所有数据库操作。</li>
     *     <li>车票状态初始化为"未支付"（{@code TicketStatusEnum.UNPAID}），等待用户支付。</li>
     *     <li>订单服务调用失败会抛出异常，触发事务回滚，已保存的车票记录会被删除。</li>
     *     <li>订单号由订单服务生成并返回，车票服务不负责订单号的生成。</li>
     * </ul>
     *
     * @param requestParam 购票请求参数（包含车次ID、出发站、到达站、乘客信息等）
     * @return 购票响应对象（包含订单号和车票详情列表）
     * @throws ServiceException 当订单服务调用失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Throwable.class)
    public TicketPurchaseRespDTO executePurchaseTickets(PurchaseTicketReqDTO requestParam) {
        // 初始化车票详情结果列表，用于返回给前端展示
        List<TicketOrderDetailRespDTO> ticketOrderDetailResults = new ArrayList<>();
        
        // 获取车次ID
        String trainId = requestParam.getTrainId();
        
        // 从缓存或数据库获取车次基本信息
        // 优先从Redis缓存读取，缓存不存在则从数据库查询并写入缓存
        // 缓存过期时间：ADVANCE_TICKET_DAY 天（提前购票天数）
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + trainId,
                TrainDO.class,
                () -> trainMapper.selectById(trainId),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        
        // 根据车次类型和购票请求，选择合适的座位
        // 返回每个乘客的座位分配结果（包含车厢号、座位号、价格等信息）
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = trainSeatTypeSelector.select(trainDO.getTrainType(), requestParam);
        
        // 将座位分配结果转换为车票实体对象列表
        // 每个车票记录包含：用户名、车次ID、车厢号、座位号、乘客ID、车票状态（未支付）
        List<TicketDO> ticketList = trainPurchaseTicketResults.stream()
                .map(each -> TicketDO.builder()
                        .username(UserContext.getUserName())                                    // 当前登录用户名
                        .trainId(Long.parseLong(requestParam.getTrainId()))                    // 车次ID
                        .carriageNumber(each.getCarriageNumber())                                // 车厢号
                        .seatNumber(each.getSeatNumber())                                        // 座位号
                        .passengerId(each.getPassengerId())                                     // 乘客ID
                        .ticketStatus(TicketStatusEnum.UNPAID.getCode())                        // 车票状态：未支付
                        .build())
                .toList();
        
        // 批量保存车票记录到数据库
        // 使用MyBatis-Plus的saveBatch方法，批量插入性能更好
        saveBatch(ticketList);
        
        // 订单服务调用结果
        Result<String> ticketOrderResult;
        
        try {
            // 构建订单项列表（用于调用订单服务）
            // 每个订单项对应一张车票，包含价格、座位、乘客信息等
            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOList = new ArrayList<>();
            
            // 遍历座位分配结果，为每个乘客构建订单项和车票详情
            trainPurchaseTicketResults.forEach(each -> {
                // 构建订单项
                // 包含订单服务需要的所有信息：价格、座位、乘客证件、联系方式等
                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO = TicketOrderItemCreateRemoteReqDTO.builder()
                        .amount(each.getAmount())                    // 车票金额（分）
                        .carriageNumber(each.getCarriageNumber())    // 车厢号
                        .seatNumber(each.getSeatNumber())            // 座位号
                        .idCard(each.getIdCard())                    // 身份证号
                        .idType(each.getIdType())                    // 证件类型
                        .phone(each.getPhone())                      // 手机号
                        .seatType(each.getSeatType())                // 座位类型（商务座、一等座等）
                        .ticketType(each.getUserType())              // 票种（成人票、儿童票等）
                        .realName(each.getRealName())                // 真实姓名
                        .build();
                
                // 构建车票详情
                // 包含前端展示需要的车票信息
                TicketOrderDetailRespDTO ticketOrderDetailRespDTO = TicketOrderDetailRespDTO.builder()
                        .amount(each.getAmount())                    // 车票金额
                        .carriageNumber(each.getCarriageNumber())    // 车厢号
                        .seatNumber(each.getSeatNumber())            // 座位号
                        .idCard(each.getIdCard())                     // 身份证号
                        .idType(each.getIdType())                     // 证件类型
                        .seatType(each.getSeatType())                // 座位类型
                        .ticketType(each.getUserType())              // 票种
                        .realName(each.getRealName())                // 真实姓名
                        .build();
                
                // 添加到列表
                orderItemCreateRemoteReqDTOList.add(orderItemCreateRemoteReqDTO);
                ticketOrderDetailResults.add(ticketOrderDetailRespDTO);
            });
            
            // 查询列车站点关系信息（获取出发时间和到达时间）
            // 根据车次ID、出发站、到达站查询站点关系，获取该路段的运行时间信息
            LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, trainId)                                    // 车次ID
                    .eq(TrainStationRelationDO::getDeparture, requestParam.getDeparture())                // 出发站编码
                    .eq(TrainStationRelationDO::getArrival, requestParam.getArrival());                  // 到达站编码
            TrainStationRelationDO trainStationRelationDO = trainStationRelationMapper.selectOne(queryWrapper);
            
            // 构建订单创建请求DTO（用于远程调用订单服务）
            // 包含订单的所有信息：出发站、到达站、车次、时间、乘客信息等
            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO = TicketOrderCreateRemoteReqDTO.builder()
                    .departure(requestParam.getDeparture())                                          // 出发站编码
                    .arrival(requestParam.getArrival())                                              // 到达站编码
                    .orderTime(new Date())                                                           // 下单时间
                    .source(SourceEnum.INTERNET.getCode())                                           // 订单来源：互联网
                    .trainNumber(trainDO.getTrainNumber())                                           // 车次号（如：G123）
                    .departureTime(trainStationRelationDO.getDepartureTime())                        // 出发时间
                    .arrivalTime(trainStationRelationDO.getArrivalTime())                            // 到达时间
                    .ridingDate(trainStationRelationDO.getDepartureTime())                            // 乘车日期（使用出发时间）
                    .userId(UserContext.getUserId())                                                 // 用户ID
                    .trainId(Long.parseLong(requestParam.getTrainId()))                              // 车次ID
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList)                                // 订单项列表
                    .build();
            
            // 调用订单服务创建订单
            // 订单服务会生成订单号并返回，如果创建失败会返回错误信息
            ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
            
            // 校验订单服务调用结果
            // 如果调用失败或订单号为空，抛出异常触发事务回滚
            if (!ticketOrderResult.isSuccess() || StrUtil.isBlank(ticketOrderResult.getData())) {
                log.error("订单服务调用失败，返回结果: {}", ticketOrderResult.getMessage());
                throw new ServiceException("订单服务调用失败");
            }
        } catch (Throwable ex) {
            // 捕获所有异常（包括订单服务调用异常）
            // 记录错误日志，然后重新抛出异常，触发事务回滚
            // 这样已保存的车票记录会被删除，保证数据一致性
            log.error("远程调用订单服务创建错误，请求参数: {}", JSON.toJSONString(requestParam), ex);
            throw ex;
        }
        
        // 构建并返回购票响应对象
        // 包含订单号（由订单服务生成）和车票详情列表（用于前端展示）
        return new TicketPurchaseRespDTO(ticketOrderResult.getData(), ticketOrderDetailResults);
    }

    @Override
    public PayInfoRespDTO getPayInfo(String orderSn) {
        return payRemoteService.getPayInfo(orderSn).getData();
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
        return trainBrandSet.stream().toList();
    }

    /**
     * 令牌桶为空时的延迟刷新和验证机制。
     * <p>
     * 当令牌桶为空（无票可售）时，不立即刷新，而是延迟10秒后异步执行刷新任务。
     * 在刷新过程中，会验证令牌桶中的数据是否准确，如果发现令牌数量与实际余票不匹配，会进行修正。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>获取分布式锁：确保同一车次只有一个线程执行刷新任务。</li>
     *     <li>延迟执行：延迟10秒后执行刷新，避免频繁刷新造成数据库压力。</li>
     *     <li>解析令牌信息：从 {@code tokenResult} 中解析出哪些座位类型的令牌为空，以及对应的令牌数量。</li>
     *     <li>查询实际余票：从数据库查询该车次路线的实际余票数量。</li>
     *     <li>数据校验：比较令牌数量与实际余票数量，如果令牌数量 <= 实际余票，说明令牌桶数据准确，删除多余的令牌。</li>
     *     <li>释放锁：无论成功与否，都要释放分布式锁。</li>
     * </ol>
     *
     * <p>为什么延迟10秒？</p>
     * <ul>
     *     <li>避免频繁刷新：如果令牌桶刚为空就立即刷新，可能短时间内多次刷新，造成数据库压力。</li>
     *     <li>等待其他操作完成：给其他可能的购票操作留出时间，避免不必要的刷新。</li>
     *     <li>批量处理：延迟执行可以将多个刷新请求合并处理，提高效率。</li>
     * </ul>
     *
     *
     * <p>校验逻辑：</p>
     * <ul>
     *     <li>如果令牌数量 <= 实际余票数量：说明令牌桶数据准确或偏少，删除多余的令牌（可能是之前残留的）。</li>
     *     <li>如果令牌数量 > 实际余票数量：说明令牌桶数据不准确，需要补充令牌（但当前代码中未实现）。</li>
     * </ul>
     *
     * @param requestParam 购票请求参数（包含车次ID、出发站、到达站等）
     * @param tokenResult  令牌获取结果（包含令牌为空的座位类型和数量信息）
     */
    private void tokenIsNullRefreshToken(PurchaseTicketReqDTO requestParam, TokenResultDTO tokenResult) {
        // 获取分布式锁
        // 基于车次ID构建锁的key，确保同一车次只有一个线程执行刷新任务
        // 防止多个线程同时刷新同一车次的令牌桶，造成数据不一致
        RLock lock = redissonClient.getLock(String.format(LOCK_TOKEN_BUCKET_ISNULL, requestParam.getTrainId()));
        
        // 尝试获取锁，如果获取不到（说明其他线程正在刷新），直接返回
        // 使用 tryLock() 非阻塞方式，避免线程等待
        if (!lock.tryLock()) {
            return;
        }
        
        // 延迟执行刷新任务
        // 延迟10秒后执行刷新任务，避免频繁刷新造成数据库压力
        // 使用异步线程池执行，不阻塞当前请求线程
        tokenIsNullRefreshExecutor.schedule(() -> {
            try {
                // 解析令牌为空的信息
                // 初始化座位类型列表和令牌数量映射
                List<Integer> seatTypes = new ArrayList<>();                    // 存储座位类型列表（如：[0, 1, 2]）
                Map<Integer, Integer> tokenCountMap = new HashMap<>();          // 存储座位类型 -> 令牌数量的映射
                
                // 解析 tokenResult 中的令牌为空信息
                // 格式：["0_5", "1_10"] 表示商务座(0)有5个令牌为空，一等座(1)有10个令牌为空
                tokenResult.getTokenIsNullSeatTypeCounts().stream()
                        // 按 "_" 分割字符串，得到 [座位类型, 令牌数量]
                        .map(each -> each.split("_"))
                        .forEach(split -> {
                            // 解析座位类型（如："0" -> 0）
                            int seatType = Integer.parseInt(split[0]);
                            seatTypes.add(seatType);
                            // 解析令牌数量（如："5" -> 5），并存储到Map中
                            tokenCountMap.put(seatType, Integer.parseInt(split[1]));
                        });
                
                // 从数据库查询实际余票数量
                // 查询该车次路线各座位类型的实际余票数量
                List<SeatTypeCountDTO> seatTypeCountDTOList = seatService.listAvailableSeatTypeCount(
                        Long.parseLong(requestParam.getTrainId()), 
                        requestParam.getDeparture(), 
                        requestParam.getArrival(), 
                        seatTypes
                );
                
                // 数据校验和修正
                // 遍历每个座位类型的实际余票数据
                for (SeatTypeCountDTO each : seatTypeCountDTOList) {
                    // 获取该座位类型在令牌桶中的令牌数量
                    Integer tokenCount = tokenCountMap.get(each.getSeatType());
                    
                    // 如果令牌数量 <= 实际余票数量，说明令牌桶数据准确或偏少
                    // 此时删除多余的令牌（可能是之前残留的无效令牌）
                    if (tokenCount <= each.getSeatCount()) {
                        // 删除令牌桶中的令牌（修正数据）
                        ticketAvailabilityTokenBucket.delTokenInBucket(requestParam);
                        // 找到一个符合条件的座位类型后，跳出循环
                        break;
                    }
                    // 如果令牌数量 > 实际余票数量，说明令牌桶数据不准确（令牌数量偏少）
                    // 理论上应该补充令牌，但当前代码中未实现此逻辑
                }
            } finally {
                // 释放分布式锁
                // 无论任务执行成功与否，都要释放锁，避免死锁
                lock.unlock();
            }
        }, 10, TimeUnit.SECONDS);  // 延迟10秒执行
    }

    @Override
    public void run(String... args) throws Exception {
        ticketService = ApplicationContextHolder.getBean(TicketService.class);
    }
}
