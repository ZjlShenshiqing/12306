package org.openzjl.index12306.framework.starter.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 布隆过滤器防缓存穿透配置属性。
 * <p>
 * 通过布隆过滤器预先判断 key 是否可能存在，避免大量无效请求直接访问数据库，
 * 从而防止缓存穿透攻击。配置包括过滤器名称、预期插入数量、误判率等参数。
 * </p>
 *
 * <p>工作原理：</p>
 * <ul>
 *     <li>数据写入时：同步将 key 添加到布隆过滤器。</li>
 *     <li>数据查询时：先检查布隆过滤器，若返回"不存在"则直接拒绝，若返回"可能存在"则继续查询缓存/数据库。</li>
 * </ul>
 *
 * <p>配置示例：</p>
 * <pre>
 * framework:
 *   cache:
 *     redis:
 *       bloom-filter:
 *         default:
 *           name: "my_bloom_filter"
 *           expected-insertions: 1000000
 *           false-probability: 0.01
 * </pre>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:06
 */
@Data
@ConfigurationProperties(prefix = BloomFilterPenetrateProperties.PREFIX)
public class BloomFilterPenetrateProperties {

    /**
     * 配置属性前缀常量。
     */
    public static final String PREFIX = "framework.cache.redis.bloom-filter.default";

    /**
     * 布隆过滤器名称
     * <p>
     * 用于在 Redis 中标识该布隆过滤器实例，支持多个过滤器并存。
     * 默认名称为 "cache_penetration_bloom_filter"。
     * </p>
     */
    private String name = "cache_penetration_bloom_filter";

    /**
     * 预期插入元素数量。
     * <p>
     * 用于计算布隆过滤器的最优位数组大小和哈希函数个数。
     * 设置过小会导致误判率上升，设置过大会浪费内存。
     * 建议根据业务数据规模合理估算，默认为 64。
     * </p>
     */
    private Long expectedInsertions = 64L;

    /**
     * 误判率（假阳性概率）。
     * <p>
     * 表示布隆过滤器判断"元素存在"时的错误概率。
     * 值越小，所需内存越大，但准确性越高。
     * 取值范围通常为 0.01 ~ 0.1，默认为 0.03（即 3% 误判率）。
     * </p>
     *
     * <p>注意：布隆过滤器不会产生假阴性（即存在的元素不会被判断为不存在）。</p>
     */
    private Double falseProbability = 0.03D;
}
