/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.job;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainDO;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.log.toolkit.EnvironmentUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_INFO;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_DETAIL;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;

/**
 * Daily train time rolling job.
 *
 * It rolls train departure/arrival/sale time to the target date,
 * rolls relation segment times accordingly, and clears related caches.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TrainDailyTimeGenerateJobHandler extends IJobHandler {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "trainDailyTimeGenerateJobHandler")
    @GetMapping("/api/ticket-service/train-daily-time/job/execute")
    @Override
    public void execute() {
        runRoll(parseTargetDate(getJobRequestParam()));
    }

    /**
     * 将车次/区段时间滚到指定日期（启动器、HTTP、XXL-JOB 共用）。
     */
    public void runRoll(LocalDate targetDepartureDate) {
        List<TrainDO> trains = trainMapper.selectList(Wrappers.emptyWrapper());
        if (trains == null || trains.isEmpty()) {
            log.info("train-daily-time job skip: no train data");
            return;
        }

        int changedCount = 0;
        for (TrainDO train : trains) {
            if (rollSingleTrainTime(train, targetDepartureDate)) {
                changedCount++;
            }
        }
        clearRelatedCache();
        log.info("train-daily-time job done: targetDate={}, changedTrainCount={}", targetDepartureDate, changedCount);
    }

    private boolean rollSingleTrainTime(TrainDO train, LocalDate targetDepartureDate) {
        Date oldDeparture = train.getDepartureTime();
        Date oldArrival = train.getArrivalTime();
        Date oldSaleTime = train.getSaleTime();
        if (oldDeparture == null || oldArrival == null || oldSaleTime == null) {
            log.warn("skip train {} due to null time field", train.getId());
            return false;
        }

        LocalDateTime oldDepartureLdt = toLocalDateTime(oldDeparture);
        LocalDateTime oldArrivalLdt = toLocalDateTime(oldArrival);
        LocalDateTime oldSaleLdt = toLocalDateTime(oldSaleTime);

        long arrivalOffsetSeconds = java.time.Duration.between(oldDepartureLdt, oldArrivalLdt).getSeconds();
        long saleOffsetSeconds = java.time.Duration.between(oldSaleLdt, oldDepartureLdt).getSeconds();

        LocalDateTime newDepartureLdt = LocalDateTime.of(targetDepartureDate, oldDepartureLdt.toLocalTime());
        LocalDateTime newArrivalLdt = newDepartureLdt.plusSeconds(arrivalOffsetSeconds);
        LocalDateTime newSaleLdt = newDepartureLdt.minusSeconds(saleOffsetSeconds);

        train.setDepartureTime(toDate(newDepartureLdt));
        train.setArrivalTime(toDate(newArrivalLdt));
        train.setSaleTime(toDate(newSaleLdt));
        int updated = trainMapper.updateById(train);
        if (updated <= 0) {
            log.warn("update train time failed, trainId={}", train.getId());
            return false;
        }

        List<TrainStationRelationDO> relationList = trainStationRelationMapper.selectList(
                Wrappers.lambdaQuery(TrainStationRelationDO.class)
                        .eq(TrainStationRelationDO::getTrainId, train.getId())
        );
        if (relationList != null) {
            for (TrainStationRelationDO relation : relationList) {
                if (relation.getDepartureTime() == null || relation.getArrivalTime() == null) {
                    continue;
                }
                LocalDateTime oldRelationDeparture = toLocalDateTime(relation.getDepartureTime());
                LocalDateTime oldRelationArrival = toLocalDateTime(relation.getArrivalTime());
                long relationDepartureOffsetSeconds = java.time.Duration.between(oldDepartureLdt, oldRelationDeparture).getSeconds();
                long relationArrivalOffsetSeconds = java.time.Duration.between(oldDepartureLdt, oldRelationArrival).getSeconds();
                relation.setDepartureTime(toDate(newDepartureLdt.plusSeconds(relationDepartureOffsetSeconds)));
                relation.setArrivalTime(toDate(newDepartureLdt.plusSeconds(relationArrivalOffsetSeconds)));
                trainStationRelationMapper.updateById(relation);
            }
        }
        return true;
    }

    private void clearRelatedCache() {
        StringRedisTemplate redisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        deleteByPattern(redisTemplate, TRAIN_INFO + "*");
        deleteByPattern(redisTemplate, TRAIN_STATION_DETAIL + "*");
        deleteByPattern(redisTemplate, TRAIN_STATION_STOPOVER_DETAIL + "*");
        deleteByPattern(redisTemplate, "index12306-ticket-service:region_train_station:*");
    }

    private void deleteByPattern(StringRedisTemplate redisTemplate, String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private LocalDate parseTargetDate(String requestParam) {
        if (StrUtil.isNotBlank(requestParam)) {
            return LocalDate.parse(requestParam);
        }
        return LocalDate.now().plusDays(1);
    }

    private LocalDateTime toLocalDateTime(Date source) {
        return source.toInstant().atZone(ZONE_ID).toLocalDateTime();
    }

    private Date toDate(LocalDateTime source) {
        return Date.from(source.atZone(ZONE_ID).toInstant());
    }

    private String getJobRequestParam() {
        return EnvironmentUtil.isDevEnvironment()
                ? getRequestHeaderParam()
                : XxlJobHelper.getJobParam();
    }

    private String getRequestHeaderParam() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return requestAttributes == null ? null : requestAttributes.getRequest().getHeader("requestParam");
    }
}
