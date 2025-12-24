package org.openzjl.index12306.biz.ticketservice.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.dao.entity.StationDO;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TicketDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.*;
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
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_REGION_TRAIN_STATION_MAPPING;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.REGION_TRAIN_STATION_MAPPING;

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
        
        // 双重检查锁定（Double-Check Locking）
        // 如果缓存未命中（count > 0），说明缓存中缺少某些站点信息，需要从数据库加载
        // 使用分布式锁确保高并发场景下只有一个线程执行数据库查询和缓存写入操作
        if (count > 0) {
            // 获取分布式锁，防止多个线程同时查询数据库并写入缓存（防止缓存击穿）
            RLock lock = redissonClient.getLock(LOCK_REGION_TRAIN_STATION_MAPPING);
            lock.lock();
            try {
                // 第二次检查缓存（加锁后再次检查）
                // 双重检查：获取锁后再次检查缓存，可能其他线程已经加载完成
                // 这是双重检查锁定模式的核心：避免重复查询数据库
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
        return null;
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

    @Override
    public void run(String... args) throws Exception {

    }
}
