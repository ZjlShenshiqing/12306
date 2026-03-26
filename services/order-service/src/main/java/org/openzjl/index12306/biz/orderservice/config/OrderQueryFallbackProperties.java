/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单查询兜底配置：当 ShardingSphere 路由查不到时，直接遍历物理表
 *
 * @author zhangjlk
 */
@Data
@Component
@ConfigurationProperties(prefix = "order.query-fallback")
public class OrderQueryFallbackProperties {

    private boolean enabled = false;

    private List<DataSourceConfig> datasources = new ArrayList<>();

    @Data
    public static class DataSourceConfig {
        private String url;
        private String username;
        private String password;
    }
}
