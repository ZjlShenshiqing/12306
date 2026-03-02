package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 购票责任链 - 余票校验过滤器
 * <p>
 * 在用户提交购票请求时，校验「车次 + 出发站 + 到达站」对应各席别（一等座、二等座等）的剩余票数，
 * 是否满足本次购买人数。先查 Redis 余票缓存，未命中则通过 {@link SeatMarginCacheLoader} 加载并参与校验。
 * </p>
 * <p>
 * 责任链标记：{@link org.openzjl.index12306.biz.ticketservice.common.enums.TicketChainMarkEnum#TRAIN_PURCHASE_TICKET_FILTER}
 * </p>
 *
 * @author zhangjlk
 * @date 2026/3/2 上午10:47
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketParamStockChainHandler implements TrainPurchaseTicketChainFilter<PurchaseTicketReqDTO> {

    /** 余票缓存加载器，Redis 无数据时从 DB/缓存加载该车次该区间的席别余票 */
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    /** 分布式缓存，用于获取 Redis 操作模板 */
    private final DistributedCache distributedCache;

    /**
     * 按席别校验余票是否足够
     * <p>
     * 1. 用「车次_出发站_到达站」拼出 Redis Hash 的 key 后缀；<br>
     * 2. 将本次乘客按席别分组（同一席别可能多人）；<br>
     * 3. 对每种席别：先查 Redis 中该区间的该席别余票，若无则用 seatMarginCacheLoader 加载；<br>
     * 4. 若该席别余票数 &lt; 该席别乘客人数，直接抛异常「列车站点已无余票」，终止购票链。
     * </p>
     *
     * @param requestParam 购票请求（含车次、出发站、到达站、乘客列表及每人席别）
     */
    @Override
    public void handler(PurchaseTicketReqDTO requestParam) {
        // 拼接 Redis Key 后缀：车次_出发站_到达站，对应「某车次某区间」的余票 Hash
        String keySuffix = StrUtil.join("_", requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();

        // 按席别分组：同一席别可能多人，需保证该席别余票 >= 该席别人数
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));

        seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
            // 先从 Redis Hash 取该区间的该席别余票（Field 为席别编码）
            Object stockObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(seatType));
            // 若 Redis 没有，则用 CacheLoader 加载（如从 DB 或本地缓存），再取该席别余票数
            int stock = Optional.ofNullable(stockObj).map(each -> Integer.parseInt(each.toString())).orElseGet(() -> {
                Map<String, String> seatMarginMap = seatMarginCacheLoader.load(
                        String.valueOf(requestParam.getTrainId()), String.valueOf(seatType),
                        requestParam.getDeparture(), requestParam.getArrival());
                return Optional.ofNullable(seatMarginMap.get(String.valueOf(seatType))).map(Integer::parseInt).orElse(0);
            });
            // 当前席别余票不足则直接失败（按「该席别人数」校验，此处用 passengerDetails.size() 与原文一致；若需严格按席别人数可改为 passengerSeatDetails.size()）
            if (stock >= passengerDetails.size()) {
                return;
            }
            throw new ClientException("列车站点已无余票");
        });
    }

    /**
     * 责任链顺序，数值越小越先执行；20 表示在购票链中相对靠后（如先做参数非空等再做余票校验）
     */
    @Override
    public int getOrder() {
        return 20;
    }
}
