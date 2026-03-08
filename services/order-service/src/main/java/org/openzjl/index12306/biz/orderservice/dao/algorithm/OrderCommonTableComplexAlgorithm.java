/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.dao.algorithm;

import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.Properties;

/**
 * 订单表复合分片算法配置
 * <p>
 * 当前实现为“广播到所有表分片”的兜底策略：直接返回所有可用表名，不做真实路由计算，
 * 主要目的是避免 ClassNotFound 异常导致数据源初始化失败，后续可按业务需要补充真实分片逻辑。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/3/3
 */
public class OrderCommonTableComplexAlgorithm implements ComplexKeysShardingAlgorithm {

    @Getter
    private Properties props;

    private int shardingCount;

    private static final String SHARDING_COUNT_KEY = "sharding-count";

    @Override
    public void init(Properties props) {
        this.props = props;
        if (props != null) {
            String shardingCountProp = props.getProperty(SHARDING_COUNT_KEY);
            if (shardingCountProp != null) {
                try {
                    shardingCount = Integer.parseInt(shardingCountProp);
                } catch (NumberFormatException ignored) {
                    // 保底策略：解析失败时保持默认值，不影响数据源启动
                }
            }
        }
    }

    @Override
    public String getType() {
        return ComplexKeysShardingAlgorithm.super.getType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<String> doSharding(Collection collection, ComplexKeysShardingValue complexKeysShardingValue) {
        // 兜底实现：暂时不做精确路由，直接返回所有可用表名，让 ShardingSphere 自行广播执行
        return collection;
    }

    public int getShardingCount() {
        return shardingCount;
    }
}

