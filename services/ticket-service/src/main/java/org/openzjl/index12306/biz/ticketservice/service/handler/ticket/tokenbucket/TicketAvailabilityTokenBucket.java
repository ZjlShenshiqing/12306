package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.tokenbucket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TokenResultDTO;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
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
        // 从缓存或数据库获取列车信息，用于后续判断列车类型和站点信息
        TrainDO trainDO = distributedCache.safeGet(
            TRAIN_INFO + requestParam.getTrainId(),
                TrainDO.class,
                () -> trainMapper.selectById(requestParam.getTrainId()),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );

        // 计算需要扣减余票的路线集合
        List<RouteDTO> routeDTOList = trainStationService
                .listTakeoutTrainStationRoute(requestParam.getTrainId(), trainDO.getStartStation(), trainDO.getEndStation());

        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 构建令牌桶的Redis Key，格式：TICKET_AVAILABILITY_TOKEN_BUCKET + 车次ID
        String tokenBucketHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
        Boolean hasKey = distributedCache.hasKey(tokenBucketHashKey);
        if (!hasKey) {
            // 拿到一个锁对象代理
            RLock lock = redissonClient.getLock(String.format(LOCK_TICKET_AVAILABILITY_TOKEN_BUCKET, requestParam.getTrainId()));

        }
        return null;
    }
}
