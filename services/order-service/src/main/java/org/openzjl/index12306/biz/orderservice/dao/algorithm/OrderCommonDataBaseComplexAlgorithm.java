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
 * 订单数据库复合分片算法配置
 *
 * @author zhangjlk
 * @date 2026/2/6 下午9:40
 */
public class OrderCommonDataBaseComplexAlgorithm implements ComplexKeysShardingAlgorithm<Comparable<?>> {

    @Getter
    private Properties props;

    private int shardingCount;
    private int tableShardingCount;

    private static final String SHARDING_COUNT_KEY = "sharding-count";
    private static final String TABLE_SHARDING_COUNT_KEY = "table-sharding-count";

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
                }
            }
            String tableShardingCountProp = props.getProperty(TABLE_SHARDING_COUNT_KEY);
            if (tableShardingCountProp != null) {
                try {
                    tableShardingCount = Integer.parseInt(tableShardingCountProp);
                } catch (NumberFormatException ignored) {
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
        if (shardingValue == null) {
            // 无分片键时只能广播，但 INSERT 不允许；这里保守返回全部，交给上层报错提示
            return availableTargetNames;
        }
        // 参考 CustomDbHashModShardingAlgorithm：
        // suffix = hash(value) % shardingCount / tableShardingCount
        long hash = Math.abs((long) shardingValue.hashCode());
        String suffix = String.valueOf(hash % shardingCount / Math.max(tableShardingCount, 1));
        for (String name : availableTargetNames) {
            if (name.endsWith("_" + suffix) || name.endsWith(suffix)) {
                return Collections.singleton(name);
            }
        }
        // 找不到则回退到全部（让上层按 ShardingSphere 默认行为处理）
        return availableTargetNames;
    }

    private Object getShardingValue(ComplexKeysShardingValue<Comparable<?>> complexKeysShardingValue) {
        Map<String, Collection<Comparable<?>>> map = complexKeysShardingValue.getColumnNameAndShardingValuesMap();
        if (map == null || map.isEmpty()) {
            return null;
        }
        // 优先用 user_id，其次 order_sn
        Object userId = firstValue(map, USER_ID_COL);
        if (userId != null) {
            return userId;
        }
        return firstValue(map, ORDER_SN_COL);
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

    public int getTableShardingCount() {
        return tableShardingCount;
    }
}
