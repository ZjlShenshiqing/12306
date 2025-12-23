package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.tokenbucket;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.dto.domain.SeatTypeCountDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.enums.VehicleTypeEnum;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TokenResultDTO;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.openzjl.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * 列车车票余量令牌桶
 *
 * 应对海量并发场景下满足并行、限流以及防超卖等场景
 *
 * @author zhangjlk
 * @date 2025/12/16 上午9:28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TicketAvailabilityTokenBucket {

    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final SeatService seatService;
    private final TrainMapper trainMapper;

    private static final String LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH = "lua/ticket_availability_token_bucket.lua";
    private static final String LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH = "lua/ticket_availability_rollback_token_bucket.lua";

    /**
     * 从令牌桶中获取令牌（检查并扣减余票）
     * <p>
     * 这是购票流程中的关键步骤，用于在高并发场景下：
     * 1. 提前过滤无票请求，减少数据库压力
     * 2. 原子性地检查和扣减余票，防止超卖
     * 3. 限流控制，避免无效请求占用系统资源
     * <p>
     * 工作流程：
     * 1. 检查令牌桶是否存在，不存在则初始化（从数据库加载余票数据）
     * 2. 使用Lua脚本原子性地检查是否有足够的令牌
     * 3. 如果有令牌，原子性地扣减对应路线的令牌数量
     * 4. 返回结果，告知是否可以继续购票流程
     * <p>
     * 返回值说明：
     * - tokenIsNull = false：有令牌，可以继续购票流程
     * - tokenIsNull = true：无令牌，余票不足，拒绝购票请求
     *
     * @param requestParam 购票请求参数，包含车次ID、出发站、到达站、乘客信息等
     * @return 令牌获取结果，包含是否有令牌、以及无令牌时的详细信息
     */
    public TokenResultDTO takeTokenFromBucket(PurchaseTicketReqDTO requestParam) {
        // 获取列车基础信息
        // 从缓存或数据库获取列车信息，优先从缓存读取，缓存不存在则从数据库查询并写入缓存
        // 获取的列车信息包含：列车类型、起始站、终点站等，用于后续判断该列车支持的座位类型和计算路线
        TrainDO trainDO = distributedCache.safeGet(
            TRAIN_INFO + requestParam.getTrainId(),
                TrainDO.class,
                () -> trainMapper.selectById(requestParam.getTrainId()),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );

        // 计算需要扣减余票的路线集合
        // 根据用户选择的出发站和到达站，计算该车次所有需要扣减余票的路线段
        // 例如：用户从A站到D站，需要扣减的路线包括：A->B、A->C、A->D、B->C、B->D、C->D
        // 这样设计是为了防止超卖：如果A->D有票，但A->B已经没票了，那么A->D也不能卖
        List<RouteDTO> routeDTOList = trainStationService
                .listTakeoutTrainStationRoute(requestParam.getTrainId(), trainDO.getStartStation(), trainDO.getEndStation());

        // 获取Redis操作模板，准备操作令牌桶
        // 获取StringRedisTemplate实例，用于后续执行Lua脚本或直接操作Redis Hash结构
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        
        // 检查令牌桶是否存在
        // 构建令牌桶的Redis Key，格式：TICKET_AVAILABILITY_TOKEN_BUCKET + 车次ID
        // 令牌桶使用Hash结构存储，Key为路线+座位类型的组合，Value为余票数量
        String tokenBucketHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
        Boolean hasKey = distributedCache.hasKey(tokenBucketHashKey);
        
        // 如果令牌桶不存在，则初始化令牌桶
        if (!hasKey) {
            // 使用分布式锁确保高并发场景下只有一个线程执行初始化操作，避免重复初始化
            // 锁的Key格式：LOCK_TICKET_AVAILABILITY_TOKEN_BUCKET + 车次ID
            RLock lock = redissonClient.getLock(String.format(LOCK_TICKET_AVAILABILITY_TOKEN_BUCKET, requestParam.getTrainId()));
            
            // 尝试获取锁，如果获取失败说明其他线程正在初始化，直接抛出异常让用户重试
            if (!lock.tryLock()) {
                throw new ServiceException("购票异常，请稍后重试");
            }
            
            try {
                // 双重检查：获取锁后再次检查令牌桶是否已存在（可能其他线程已经初始化完成）
                Boolean hasKeyTwo = distributedCache.hasKey(tokenBucketHashKey);
                if (!hasKeyTwo) {
                    // 获取该列车类型支持的座位类型列表
                    // 根据列车类型（如高速铁路、动车、普通车）获取该类型支持的座位类型编码列表
                    // 例如：高速铁路支持商务座、一等座、二等座
                    List<Integer> seatTypes = VehicleTypeEnum.findSeatTypeByCode(trainDO.getTrainType());
                    
                    // 初始化令牌映射表
                    // 创建一个Map用于存储所有路线和座位类型的余票数量
                    // Key格式：出发站_到达站_座位类型编码（如：1001_1002_0）
                    // Value：该路线该座位类型的余票数量
                    Map<String, String> ticketAvailabilityTokenMap = new HashMap<>();
                    
                    // 遍历所有需要扣减的路线，查询每条路线的余票信息
                    for (RouteDTO routeDTO : routeDTOList) {
                        // 查询该路线所有座位类型的可用余票数量
                        // 参数：车次ID、出发站、到达站、座位类型列表
                        List<SeatTypeCountDTO> seatTypeCountList = seatService.listAvailableSeatTypeCount(
                                Long.parseLong(requestParam.getTrainId()), 
                                routeDTO.getStartStation(), 
                                routeDTO.getEndStation(), 
                                seatTypes
                        );
                        
                        // 将每条路线的每种座位类型的余票数量存入映射表
                        for (SeatTypeCountDTO seatTypeCount : seatTypeCountList) {
                            // 构建缓存Key：出发站_到达站_座位类型编码
                            // 例如：1001_1002_0 表示从1001站到1002站的商务座（编码0）
                            String buildCacheKey = StrUtil.join("_", routeDTO.getStartStation(), routeDTO.getEndStation(), seatTypeCount.getSeatType());
                            // 将余票数量存入映射表，Value转为String类型（Redis Hash的Value必须是String）
                            ticketAvailabilityTokenMap.put(buildCacheKey, String.valueOf(seatTypeCount.getSeatCount()));
                        }
                    }

                    // 将令牌映射表批量写入Redis
                    // 使用Hash结构批量写入，一次性将所有路线和座位类型的余票信息存入Redis
                    // 这样后续的扣减操作可以直接在Redis中原子性地进行，无需访问数据库
                    stringRedisTemplate.opsForHash().putAll(TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId(), ticketAvailabilityTokenMap);
                }
            } finally {
                // 无论初始化成功与否，都要释放锁，避免死锁
                lock.unlock();
            }
        }

        DefaultRedisScript<String> actual = null;
        return null;
    }
}
