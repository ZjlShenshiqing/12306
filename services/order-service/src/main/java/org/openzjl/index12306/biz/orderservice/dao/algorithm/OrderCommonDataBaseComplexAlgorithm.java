package org.openzjl.index12306.biz.orderservice.dao.algorithm;

import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * 订单数据库复合分片算法配置
 *
 * @author zhangjlk
 * @date 2026/2/6 下午9:40
 */
public class OrderCommonDataBaseComplexAlgorithm implements ComplexKeysShardingAlgorithm {

    @Getter
    private Properties props;

    private int shardingCount;
    private int tableShardingCount;

    private static final String SHARDING_COUNT_KEY = "sharding_count";
    private static final String TABLE_SHARDING_COUNT_KEY = "table_sharding-count";

    @Override
    public void init(Properties props) {
        this.props = props;

    }

    @Override
    public String getType() {
        return ComplexKeysShardingAlgorithm.super.getType();
    }

    @Override
    public Collection<String> doSharding(Collection collection, ComplexKeysShardingValue complexKeysShardingValue) {
        return List.of();
    }

    public int getShardingCount() {
        return shardingCount;
    }

    public int getTableShardingCount() {
        return tableShardingCount;
    }
}
