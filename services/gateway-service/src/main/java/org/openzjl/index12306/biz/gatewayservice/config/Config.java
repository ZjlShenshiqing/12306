/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.gatewayservice.config;

import lombok.Data;

import java.util.List;

/**
 * 过滤器配置
 *
 * @author zhangjlk
 * @date 2025/10/12 16:59
 */
@Data
public class Config {

    /**
     * 黑名单前置路径
     */
    private List<String> blackPathPre;
}
