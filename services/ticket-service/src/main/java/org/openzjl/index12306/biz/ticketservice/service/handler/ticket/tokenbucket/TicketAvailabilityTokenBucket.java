package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.tokenbucket;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.dto.domain.SeatTypeCountDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketPassengerDetailDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.ticketservice.enums.VehicleTypeEnum;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TokenResultDTO;
import org.openzjl.index12306.framework.starter.bases.Singleton;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.log.toolkit.Assert;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
     * 完整工作流程：
     * <p>
     * 第一阶段：令牌桶初始化（如果不存在）
     * 1. 获取列车基础信息（从缓存或数据库）
     * 2. 计算需要扣减余票的所有路线段（防止超卖的关键）
     * 3. 检查令牌桶是否存在，不存在则使用分布式锁进行初始化
     * 4. 初始化过程：查询数据库获取所有路线和座位类型的余票数量，存入Redis Hash结构
     * <p>
     * 第二阶段：执行令牌扣减（使用Lua脚本保证原子性）
     * 1. 加载Lua脚本（使用单例模式，避免重复加载）
     * 2. 统计乘客按座位类型的分组和数量（例如：2张商务座、3张一等座）
     * 3. 将统计结果转换为JSON格式，供Lua脚本使用
     * 4. 计算用户选择的路线段（出发站到到达站的所有中间路线）
     * 5. 执行Lua脚本，原子性地检查并扣减令牌：
     *    - 检查所有相关路线的余票是否充足
     *    - 如果充足，原子性地扣减对应路线的令牌数量
     *    - 返回扣减结果
     * 6. 解析Lua脚本返回的结果，返回给调用方
     * <p>
     * 返回值说明：
     * - tokenIsNull = false：有令牌，已成功扣减，可以继续购票流程
     * - tokenIsNull = true：无令牌，余票不足，拒绝购票请求
     * <p>
     * 核心设计思想：
     * 1. 令牌桶模式：将余票抽象为令牌，提前在Redis中维护，避免频繁查询数据库
     * 2. 原子性操作：使用Lua脚本保证检查和扣减的原子性，防止超卖
     * 3. 路线段扣减：用户购买A->D的票，需要扣减A->B、A->C、A->D、B->C、B->D、C->D所有路线段的余票
     *    这样确保不会出现超卖：即使A->D有票，但如果A->B已经没票了，A->D也不能卖
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

        // 执行令牌扣减操作
        
        // 加载Lua脚本（使用单例模式，避免重复加载和解析脚本文件）
        // Lua脚本用于原子性地检查和扣减令牌，确保在高并发场景下不会出现超卖问题
        // Singleton.get() 确保同一个脚本路径只加载一次，提高性能
        DefaultRedisScript<String> actual = Singleton.get(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH, () -> {
            DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
            // 从classpath加载Lua脚本文件
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH)));
            // 设置返回类型为String（Lua脚本返回JSON字符串）
            redisScript.setResultType(String.class);
            return redisScript;
        });
        // 断言脚本加载成功，如果为null则抛出异常
        Assert.notNull(actual);
        
        // 统计乘客按座位类型的分组和数量
        // 例如：如果用户购买2张商务座和3张一等座，则结果为：{0: 2, 1: 3}
        // 其中0表示商务座编码，1表示一等座编码
        Map<Integer, Long> seatTypeCountMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType, Collectors.counting()));
        
        // 将统计结果转换为JSONArray格式，供Lua脚本使用
        // 转换后的格式示例：[{"seatType":"0","count":"2"},{"seatType":"1","count":"3"}]
        // 这样Lua脚本可以方便地解析和处理
        JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                .map(entry -> {
                    JSONObject jsonObject = new JSONObject();
                    // 座位类型编码（如：0=商务座，1=一等座）
                    jsonObject.put("seatType", String.valueOf(entry.getKey()));
                    // 该座位类型的购买数量
                    jsonObject.put("count", String.valueOf(entry.getValue()));
                    return jsonObject;
                })
                .collect(Collectors.toCollection(JSONArray::new));
        
        // 计算用户选择的路线段（出发站到到达站的所有中间路线）
        // 例如：用户从A站到D站，需要扣减的路线包括：A->B、A->C、A->D、B->C、B->D、C->D
        // 这是防止超卖的关键：必须确保所有相关路线段都有足够的余票
        List<RouteDTO> takeoutRouteDTOList = trainStationService
                .listTakeoutTrainStationRoute(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
        
        // 构建Lua脚本的Key，用于标识本次购票请求的路线
        // 格式：出发站_到达站（如：1001_1002）
        // 这个Key会传递给Lua脚本，用于在令牌桶中查找对应的余票信息
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());
        
        // 执行Lua脚本，原子性地检查并扣减令牌
        // 参数说明：
        // - actual: Lua脚本对象
        // - Lists.newArrayList(tokenBucketHashKey, luaScriptKey): Redis的Key列表
        //   - tokenBucketHashKey: 令牌桶的Hash Key（包含该车次所有路线的余票信息）
        //   - luaScriptKey: 本次购票的路线标识（出发站_到达站）
        // - JSON.toJSONString(seatTypeCountArray): 座位类型和数量的JSON字符串
        // - JSON.toJSONString(takeoutRouteDTOList): 需要扣减的路线段列表的JSON字符串
        // 
        // Lua脚本的执行逻辑（在Redis服务器端原子性执行）：
        // 1. 检查所有相关路线段的余票是否充足
        // 2. 如果充足，原子性地扣减对应路线的令牌数量
        // 3. 返回扣减结果（JSON格式的TokenResultDTO字符串）
        // 
        // 原子性保证：整个检查和扣减过程在Redis服务器端一次性完成，不会出现并发问题
        String resultStr = stringRedisTemplate
                .execute(actual, Lists.newArrayList(tokenBucketHashKey, luaScriptKey), JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(takeoutRouteDTOList));
        
        // 解析Lua脚本返回的结果
        // Lua脚本返回的是JSON字符串，需要反序列化为TokenResultDTO对象
        TokenResultDTO result = JSON.parseObject(resultStr, TokenResultDTO.class);
        
        // 处理结果并返回
        // 如果结果为null（可能是Lua脚本执行异常或返回空），则返回无令牌的结果
        // 否则返回Lua脚本的执行结果（包含是否成功扣减令牌的信息）
        return result == null
                ? TokenResultDTO.builder().tokenIsNull(Boolean.TRUE).build()
                : result;
    }

    /**
     * 回滚列车余量令牌（将已扣减的令牌重新加回到令牌桶中）
     * <p>
     * 使用场景：
     * 1. 订单取消：用户主动取消订单，需要将已扣减的余票重新释放
     * 2. 订单超时未支付：订单创建后长时间未支付，系统自动取消订单并释放余票
     * 3. 订单支付失败：支付过程中出现异常，需要回滚已扣减的令牌
     * <p>
     * 工作流程：
     * 1. 加载回滚令牌的Lua脚本（使用单例模式，避免重复加载）
     * 2. 从订单信息中提取乘客详情，统计按座位类型的分组和数量
     * 3. 将统计结果转换为JSON格式，供Lua脚本使用
     * 4. 计算订单涉及的路线段（出发站到到达站的所有中间路线）
     * 5. 执行Lua脚本，原子性地将令牌加回到令牌桶中
     * 6. 检查执行结果，如果失败则记录日志并抛出异常
     * <p>
     * 核心设计思想：
     * 1. 原子性操作：使用Lua脚本保证回滚操作的原子性，确保在高并发场景下不会出现数据不一致
     * 2. 路线段回滚：订单取消时，需要将之前扣减的所有相关路线段的令牌都加回去
     *    例如：用户购买了A->D的票，回滚时需要将A->B、A->C、A->D、B->C、B->D、C->D所有路线段的令牌都加回去
     * 3. 返回值检查：Lua脚本返回0表示成功，非0或null表示失败，需要记录日志并抛出异常
     *
     * @param requestParam 订单详情，包含车次ID、出发站、到达站、乘客信息等，用于回滚令牌
     * @throws ServiceException 如果回滚失败，抛出业务异常
     */
    public void rollbackInBucket(TicketOrderDetailRespDTO requestParam) {
        // 加载回滚令牌的Lua脚本（使用单例模式，避免重复加载和解析脚本文件）
        // Lua脚本用于原子性地将令牌加回到令牌桶中，确保在高并发场景下不会出现数据不一致
        DefaultRedisScript<Long> actual = Singleton.get(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH, () -> {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            // 从classpath加载Lua脚本文件
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH)));
            // 设置返回类型为Long（Lua脚本返回0表示成功，非0表示失败）
            redisScript.setResultType(Long.class);
            return redisScript;
        });

        // 断言脚本加载成功，如果为null则抛出异常
        Assert.notNull(actual);
        
        // 从订单信息中提取乘客详情列表
        List<TicketOrderPassengerDetailRespDTO> passengerDetails = requestParam.getPassengerDetails();
        
        // 统计乘客按座位类型的分组和数量
        // 例如：如果订单包含2张商务座和3张一等座，则结果为：{0: 2, 1: 3}
        // 其中0表示商务座编码，1表示一等座编码
        Map<Integer, Long> seatTypeCountMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType, Collectors.counting()));
        
        // 将统计结果转换为JSONArray格式，供Lua脚本使用
        // 转换后的格式示例：[{"seatType":"0","count":"2"},{"seatType":"1","count":"3"}]
        // 这样Lua脚本可以方便地解析和处理
        JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                .map(entry -> {
                    JSONObject jsonObject = new JSONObject();
                    // 座位类型编码（如：0=商务座，1=一等座）
                    jsonObject.put("seatType", String.valueOf(entry.getKey()));
                    // 该座位类型的回滚数量（需要加回的令牌数量）
                    jsonObject.put("count", String.valueOf(entry.getValue()));
                    return jsonObject;
                })
                .collect(Collectors.toCollection(JSONArray::new));
        
        // 获取Redis操作模板，用于执行Lua脚本
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        
        // 构建令牌桶的Redis Key，格式：TICKET_AVAILABILITY_TOKEN_BUCKET + 车次ID
        // 这个Key指向存储该车次所有路线和座位类型余票信息的Hash结构
        String actualHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
        
        // 构建Lua脚本的Key，用于标识本次回滚的路线
        // 格式：出发站_到达站（如：1001_1002）
        // 这个Key会传递给Lua脚本，用于在令牌桶中查找对应的余票信息
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());
        
        // 计算订单涉及的路线段（出发站到到达站的所有中间路线）
        // 例如：订单是从A站到D站，需要回滚的路线包括：A->B、A->C、A->D、B->C、B->D、C->D
        // 这是回滚的关键：必须将之前扣减的所有相关路线段的令牌都加回去
        List<RouteDTO> takeoutTrainStationRoute = trainStationService.listTakeoutTrainStationRoute(
                String.valueOf(requestParam.getTrainId()), requestParam.getDeparture(), requestParam.getArrival()
        );
        
        // 执行Lua脚本，原子性地将令牌加回到令牌桶中
        // 参数说明：
        // - actual: Lua脚本对象
        // - Lists.newArrayList(actualHashKey, luaScriptKey): Redis的Key列表
        //   - actualHashKey: 令牌桶的Hash Key（包含该车次所有路线的余票信息）
        //   - luaScriptKey: 本次回滚的路线标识（出发站_到达站）
        // - JSON.toJSONString(seatTypeCountArray): 座位类型和数量的JSON字符串
        // - JSON.toJSONString(takeoutTrainStationRoute): 需要回滚的路线段列表的JSON字符串
        // 
        // Lua脚本的执行逻辑（在Redis服务器端原子性执行）：
        // 1. 遍历所有相关路线段，将对应座位类型的令牌数量加回去
        // 2. 返回执行结果：0表示成功，非0表示失败
        // 
        // 原子性保证：整个回滚过程在Redis服务器端一次性完成，不会出现并发问题
        Long result = stringRedisTemplate.execute(actual, Lists.newArrayList(actualHashKey, luaScriptKey), JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(takeoutTrainStationRoute));
        
        // 检查执行结果
        // Lua脚本返回0表示成功，非0或null表示失败
        // 如果失败，记录错误日志（包含订单信息）并抛出业务异常
        if (result == null || !Objects.equals(result, 0L)) {
            log.error("回滚列车余票令牌失败，订单信息: {}", JSON.toJSONString(requestParam));
            throw new ServiceException("回滚列车余票令牌失败！");
        }
    }

    /**
     * 删除令牌
     * 一般在令牌与数据库不一致的情况下触发
     *
     * @param requestParam 删除令牌容器参数
     */
    public void delTokenInBucket(PurchaseTicketReqDTO requestParam) {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        String tokenBucketHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
        stringRedisTemplate.delete(tokenBucketHashKey);
    }

    public void putTokenInBucket() {

    }

    public void initializeTokens() {

    }
}
