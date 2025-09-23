package org.openzjl.index12306.framework.starter.database.algorithm.sharding;

import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.sharding.algorithm.sharding.ShardingAutoTableAlgorithmUtil;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import org.apache.shardingsphere.sharding.exception.algorithm.sharding.ShardingAlgorithmInitializationException;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * 数据库分片算法（自定义哈希取模）
 *
 * 设计说明：
 * 1. 该算法用于【按分片键】将请求路由到具体的数据源/库（或表后缀）。
 * 2. 精确分片：对分片键做 hash，再基于总分片数取模，最后结合每个库下表的分片数，计算库的后缀并匹配可用目标名。
 * 3. 范围分片：简单返回所有可用目标（保守策略，适合范围查询时广播到所有分片）。
 *
 * 参数约定（由 ShardingSphere 配置传入）：
 * - sharding-count：总分片数量（= 库数 × 每库表分片数）。
 * - table-sharding-count：每个库中表的分片数量。
 *
 * 基本思路：
 * - hash(shardingValue) % shardingCount 得到全局分片号；
 * - 再除以 tableShardingCount 得到库索引（即库后缀），据此从 availableTargetNames 中匹配命中的库/数据源名。
 *
 * @author zhangjlk
 * @date 2025/9/22 20:32
 */
public final class CustomDbHashModShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private static final String SHARDING_COUNT_KEY = "sharding-count";
    private static final String TABLE_SHARDING_COUNT_KEY = "table-sharding-count";

    // 分片总数（所有库 × 每库的表分片数）
    private int shardingCount;
    // 每个库中的表分片数量
    private int tableShardingCount;

    @Override
    public void init(final Properties props) {
        // 从配置中读取并校验必要参数
        shardingCount = getShardingCount(props);
        tableShardingCount = getTableShardingCount(props);
    }

    @Override
    public String doSharding(final Collection<String> availableTargetNames, final PreciseShardingValue<Comparable<?>> shardingValue) {
        // 精确分片：
        // 1) 计算全局分片号：hash(value) % shardingCount
        // 2) 计算库后缀：全局分片号 / tableShardingCount
        // 3) 在 availableTargetNames 中查找以该后缀结尾的目标名
        String suffix = String.valueOf(hashShardingValue(shardingValue.getValue()) % shardingCount / tableShardingCount);
        return ShardingAutoTableAlgorithmUtil
                .findMatchedTargetName(availableTargetNames, suffix, shardingValue.getDataNodeInfo())
                .orElse(null);
    }

    @Override
    public Collection<String> doSharding(final Collection<String> availableTargetNames, final RangeShardingValue<Comparable<?>> shardingValue) {
        // 范围分片：保守返回所有目标，让上层在执行计划中进行裁剪/路由
        return availableTargetNames;
    }

    private int getTableShardingCount(final Properties props) {
        // 校验并读取每库表分片数
        ShardingSpherePreconditions.checkState(props.containsKey(TABLE_SHARDING_COUNT_KEY), () -> new ShardingAlgorithmInitializationException(getType(), "Table sharding count cannot be null"));
        return Integer.parseInt(props.getProperty(TABLE_SHARDING_COUNT_KEY));
    }

    private int getShardingCount(final Properties props) {
        // 校验并读取总分片数
        ShardingSpherePreconditions.checkState(props.containsKey(SHARDING_COUNT_KEY), () -> new ShardingAlgorithmInitializationException(getType(), "ShardingCount count cannot be null"));
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    private long hashShardingValue(final Object shardingValue) {
        // 使用对象的 hashCode 并取绝对值，得到非负哈希值
        return Math.abs((long) shardingValue.hashCode());
    }

    @Override
    public String getType() {
        // 声明算法类型标识，供 ShardingSphere 识别
        return "CLASS_BASED";
    }
}
