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

import java.time.LocalDate;

/**
 * 启动时自动执行车次时间滚动（默认滚到当天），避免依赖 XXL-JOB 或系统 cron。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "ticket.job.train-daily-time",
        name = "run-on-startup",
        havingValue = "true",
        matchIfMissing = true
)
public class TrainDailyTimeStartupRunner implements ApplicationRunner {

    private final TrainDailyTimeGenerateJobHandler trainDailyTimeGenerateJobHandler;

    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now();
        log.info("train-daily-time: run on startup, targetDate={}", today);
        trainDailyTimeGenerateJobHandler.runRoll(today);
    }
}
