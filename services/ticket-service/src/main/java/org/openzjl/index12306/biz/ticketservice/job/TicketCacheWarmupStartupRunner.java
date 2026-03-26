/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时预热余票/经停/地区等缓存，需在 {@link TrainDailyTimeStartupRunner} 之后执行。
 */
@Slf4j
@Component
@Order(2)
@SuppressWarnings("deprecation")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "ticket.job",
        name = "warm-cache-on-startup",
        havingValue = "true",
        matchIfMissing = true
)
public class TicketCacheWarmupStartupRunner implements ApplicationRunner {

    private final TrainStationRemainingTicketJobHandler trainStationRemainingTicketJobHandler;
    private final TrainStationJobHandler trainStationJobHandler;
    private final TrainStationDetailJobHandler trainStationDetailJobHandler;
    private final RegionTrainStationJobHandler regionTrainStationJobHandler;

    @Override
    public void run(ApplicationArguments args) {
        log.info("ticket cache warmup: remaining ticket → station → detail → region");
        trainStationRemainingTicketJobHandler.execute();
        trainStationJobHandler.execute();
        trainStationDetailJobHandler.execute();
        regionTrainStationJobHandler.execute();
        log.info("ticket cache warmup done");
    }
}
