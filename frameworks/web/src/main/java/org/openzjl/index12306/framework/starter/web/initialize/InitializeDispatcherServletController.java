/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.web.initialize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.openzjl.index12306.framework.starter.web.config.WebAutoConfiguration.INITIALIZE_PATH;

/**
 * 初始化DispatcherServlet
 * @author zhangjlk
 * @date 2025/10/4 14:46
 */
@Slf4j(topic = "初始化 DispatcherServlet")
@RestController
public final class InitializeDispatcherServletController {

    @GetMapping(INITIALIZE_PATH)
    public void initializeDispatcherServlet() {
        log.info("已触发 DispatcherServlet 初始化，用于优化首次接口响应速度");
    }
}
