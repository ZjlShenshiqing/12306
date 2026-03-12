/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.dao.algorithm;

import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 订单表复合分片算法配置
 * <p>
 * 复合分片算法：优先使用 user_id，其次使用 order_sn 做 hash 取模，路由到单表。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/3/3
 */
public class OrderCommonTableComplexAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    @Getter
    private Properties props;

    private int shardingCount;

    private static final String SHARDING_COUNT_KEY = "sharding-count";
    private static final String USER_ID_COL = "user_id";
    private static final String ORDER_SN_COL = "order_sn";

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
    public Collection<String> doSharding(Collection<String> availableTargetNames, ComplexKeysShardingValue<Comparable<?>> complexKeysShardingValue) {
        if (availableTargetNames == null || availableTargetNames.isEmpty() || complexKeysShardingValue == null) {
            return availableTargetNames;
        }
        Object shardingValue = getShardingValue(complexKeysShardingValue);
        if (shardingValue == null || shardingCount <= 0) {
            return availableTargetNames;
        }
        long hash = Math.abs((long) shardingValue.hashCode());
        String suffix = String.valueOf(hash % shardingCount);
        for (String name : availableTargetNames) {
            if (name.endsWith("_" + suffix) || name.endsWith(suffix)) {
                return Collections.singleton(name);
            }
        }
        return availableTargetNames;
    }

    private Object getShardingValue(ComplexKeysShardingValue<Comparable<?>> complexKeysShardingValue) {
        Map<String, Collection<Comparable<?>>> map = complexKeysShardingValue.getColumnNameAndShardingValuesMap();
        if (map == null || map.isEmpty()) {
            return null;
        }
        Object orderSn = firstValue(map, ORDER_SN_COL);
        Object userId = firstValue(map, USER_ID_COL);
        if (orderSn != null && userId != null) {
            return orderSn;
        }
        if (orderSn != null && userId == null) {
            return null;
        }
        if (userId != null) {
            return userId;
        }
        return null;
    }

    private Object firstValue(Map<String, Collection<Comparable<?>>> map, String key) {
        Set<Map.Entry<String, Collection<Comparable<?>>>> entries = map.entrySet();
        for (Map.Entry<String, Collection<Comparable<?>>> entry : entries) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key) && entry.getValue() != null) {
                for (Comparable<?> v : entry.getValue()) {
                    return v;
                }
            }
        }
        return null;
    }

    public int getShardingCount() {
        return shardingCount;
    }
}

