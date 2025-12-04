package org.openzjl.index12306.biz.ticketservice.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import org.openzjl.index12306.biz.ticketservice.dao.entity.SeatDO;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.log.toolkit.ThreadUtil;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 座位控制器 - 用于测试环境使用，将指定车次的所有座位状态重置为Avaliable，也就是可用
 * 删除车次所有的余票、令牌桶缓存
 *
 * @author zhangjlk
 * @date 2025/11/22 16:28
 */
@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatMapper seatMapper;
    private final DistributedCache distributedCache;
    private final TrainStationRelationMapper trainStationRelationMapper;

    /**
     * 座位重置
     */
    @PostMapping("/api/ticket-service/temp/seat/reset")
    public Result<Void> resetSeat(@RequestParam String trainId) { // 建议将方法名 purchaseTickets 改为 resetSeat

        // 准备数据库更新对象
        SeatDO seatDO = new SeatDO();
        seatDO.setSeatStatus(SeatStatusEnum.AVAILABLE.getCode()); // 设置状态为“可用”

        // 更新数据库：将该车次下所有记录的座位状态更新为 AVAILABLE
        // 类似: UPDATE seat SET seat_status = 0 WHERE train_id = 'trainId'
        seatMapper.update(seatDO, Wrappers.lambdaUpdate(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId));

        // 线程休眠 5 秒
        // 目的：这是一个简易的“延时双删”策略或等待主从数据库同步。
        // 在更新完数据库后，等待一段时间，确保此时可能正在进行的旧数据查询请求结束，
        // 或者等待数据库主从复制完成，防止立刻删缓存后，并发请求又把旧数据读入缓存。
        ThreadUtil.sleep(5000);

        // 获取 Redis 操作实例
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // 查询该车次涉及的所有站点关系（用于拼接缓存 Key）
        // 需要知道这个车次经过哪些区间（例如：北京-上海，北京-南京等），才能精准删除对应的余票缓存
        List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(
                Wrappers.lambdaQuery(TrainStationRelationDO.class)
                        .eq(TrainStationRelationDO::getTrainId, trainId)
        );

        // 遍历所有站点区间，删除余票缓存
        for (TrainStationRelationDO trainStationRelationDO : trainStationRelationDOList) {
            // 拼接 Key，格式如：ticket_service:remaining_ticket:G123_北京_上海
            String keySuffix = StrUtil.join("_",
                    trainStationRelationDO.getTrainId(),
                    trainStationRelationDO.getDeparture(),
                    trainStationRelationDO.getArrival());

            // 执行删除，下次查询时会从数据库重新加载最新的余票数量
            stringRedisTemplate.delete(TRAIN_STATION_REMAINING_TICKET + keySuffix);
        }

        // 删除令牌桶（Token Bucket）缓存
        // 令牌桶通常用于高并发下的库存流控。删除后，系统会重新初始化令牌桶，确保令牌数量与重置后的座位一致。
        stringRedisTemplate.delete(TICKET_AVAILABILITY_TOKEN_BUCKET + trainId);

        // 8. 返回操作成功
        return Results.success();
    }
}
