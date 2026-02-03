package org.openzjl.index12306.biz.ticketservice.canal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import org.openzjl.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import org.openzjl.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractExecuteStrategy;
import org.redisson.transaction.operation.map.MapOperation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.*;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 列车余票缓存更新处理器
 * <p>
 * 实现 AbstractExecuteStrategy 接口，用于处理列车座位状态变更事件，更新余票缓存。
 * 当座位状态变为可用（AVAILABLE）或已锁定（LOCKED）时，会触发余票缓存的更新。
 * </p>
 *
 * <p><strong>设计用途：</strong></p>
 * <ul>
 *   <li>处理 Canal 数据同步中的座位状态变更事件</li>
 *   <li>根据座位状态变更更新余票缓存</li>
 *   <li>确保缓存中的余票数量与数据库中的实际座位状态保持一致</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2026/2/3 19:24
 */
@Component
@RequiredArgsConstructor
public class TicketAvailabilityCacheUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {

    private final DistributedCache distributedCache;

    /**
     * 执行余票缓存更新逻辑
     * <p>
     * 处理 Canal 数据同步中的座位状态变更事件，当座位状态变为可用或已锁定时，准备更新余票缓存。
     * </p>
     *
     * @param message Canal 二进制日志事件
     *               <ul>
     *                 <li>包含座位数据的变更信息，包括变更前的旧数据和变更后的当前数据</li>
     *               </ul>
     */
    @Override
    public void execute(CanalBinlogEvent message) {
        // 初始化数据容器
        List<Map<String, Object>> messageDataList = new ArrayList<>();
        List<Map<String, Object>> actualOldDataList = new ArrayList<>();

        // 过滤有效数据
        for (int i = 0; i < message.getOld().size(); i++) {
            Map<String, Object> oldDataMap = message.getOld().get(i);
            // 检查旧数据中是否包含座位状态字段且不为空
            if (oldDataMap.get("seat_status") != null && StrUtil.isNotBlank(oldDataMap.get("seat_status").toString())) {
                Map<String, Object> currentDataMap = message.getData().get(i);
                // 检查当前数据的座位状态是否为可用或已锁定
                if (StrUtil.equalsAny(currentDataMap.get("seat_status").toString(), String.valueOf(SeatStatusEnum.AVAILABLE.getCode()), String.valueOf(SeatStatusEnum.LOCKED.getCode()))) {
                    actualOldDataList.add(oldDataMap);
                    messageDataList.add(currentDataMap);
                }
            }
        }

        // 检查数据有效性
        if (CollUtil.isEmpty(messageDataList) || CollUtil.isEmpty(actualOldDataList)) {
            return;
        }

        // 准备缓存更新
        Map<String, Map<Integer, Integer>> cacheChangeKeyMap = new HashMap<>();
        for (int i = 0; i < messageDataList.size(); i++) {
            Map<String, Object> each = messageDataList.get(i);
            Map<String, Object> actualOldData = actualOldDataList.get(i);
            String seatStatus = actualOldData.get("seat_status").toString();
            int increment = Objects.equals(seatStatus, "0") ? -1 : 1;
            String trainId = each.get("train_id").toString();
            String hashCacheKey = TRAIN_STATION_REMAINING_TICKET + trainId + "_" + each.get("start_station") + "_" + each.get("end_station");
            Map<Integer, Integer> seatTypeMap = cacheChangeKeyMap.get(hashCacheKey);
            if (CollUtil.isEmpty(seatTypeMap)) {
                seatTypeMap = new HashMap<>();
            }
            Integer seatType = Integer.parseInt(each.get("seat_type").toString());
            Integer num = seatTypeMap.get(seatType);
            seatTypeMap.put(seatType, num == null ? increment : num + increment);
            cacheChangeKeyMap.put(hashCacheKey, seatTypeMap);
        }
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        cacheChangeKeyMap.forEach((cacheKey, cacheVal) -> cacheVal.forEach((seatType, num) -> instance.opsForHash().increment(cacheKey, String.valueOf(seatType), num)));
    }

    /**
     * 获取策略标记
     * <p>
     * 返回座位表的实际表名，用于策略选择。
     * </p>
     *
     * @return 座位表的实际表名
     *         <ul>
     *           <li>返回值：{@link CanalExecuteStrategyMarkEnum#T_SEAT} 的实际表名</li>
     *         </ul>
     */
    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_SEAT.getActualTable();
    }
}
