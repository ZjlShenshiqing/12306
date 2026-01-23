package org.openzjl.index12306.biz.payservice.service.Impl.payid;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 支付 ID 全局唯一生成器管理类
 * <p>
 * 该类是支付服务的核心组件，负责初始化并管理 {@link DistributedIdGenerator} 实例，
 * 为支付流程生成全局唯一的流水号。采用分布式设计，支持多实例部署场景。
 * </p>
 * 
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>支付下单时生成唯一支付流水号</li>
 *   <li>退款申请时生成唯一退款流水号</li>
 *   <li>其他需要全局唯一标识符的支付相关操作</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2026/1/23 17:10
 */
@Component
@RequiredArgsConstructor
public final class PayIdGeneratorManager implements InitializingBean {

    /**
     * 分布式 ID 生成器实例
     * <p>
     * 静态单例模式，确保整个应用中只有一个 ID 生成器实例，
     * 由 {@link #afterPropertiesSet()} 方法初始化后供静态方法使用。
     */
    private static DistributedIdGenerator DISTRIBUTED_ID_GENERATOR;
    
    /**
     * Redisson 客户端
     * <p>
     * 用于获取分布式锁，保证多实例部署时初始化过程的安全性。
     */
    private final RedissonClient redissonClient;
    
    /**
     * 分布式缓存客户端
     * <p>
     * 用于获取 StringRedisTemplate 实例，操作 Redis 自增键分配节点编号。
     */
    private final DistributedCache distributedCache;

    /**
     * 生成支付全局唯一流水号
     *
     * @param orderSn 订单号
     *                <ul>
     *                  <li>一般为业务订单号或子订单号</li>
     *                  <li>需保证长度大于等于 6 位，否则会抛出字符串索引越界异常</li>
     *                  <li>建议使用系统生成的规范订单号，确保唯一性和长度要求</li>
     *                </ul>
     * @return 支付流水号
     *         <p>
     *         生成规则：
     *         <ul>
     *           <li><strong>前缀部分：</strong>由 {@link DistributedIdGenerator#generateId()} 生成，
     *               包含时间戳、节点编号和序列号，保证全局唯一且趋势递增</li>
     *           <li><strong>后缀部分：</strong>取订单号 {@code orderSn} 的后 6 位，
     *               便于通过流水号快速关联到原始业务订单</li>
     *         </ul>
     *         示例：202601231234567890123456（前缀） + 789012（订单号后6位）
     *         </p>
     * @throws NullPointerException 如果 {@code orderSn} 为 null
     * @throws IndexOutOfBoundsException 如果 {@code orderSn} 长度小于 6 位
     */
    public static String generateId(String orderSn) {
        // 调用分布式 ID 生成器生成前缀，拼接订单号后 6 位作为后缀
        return DISTRIBUTED_ID_GENERATOR.generateId() + orderSn.substring(orderSn.length() - 6);
    }

    /**
     * Spring 容器属性注入完成后的初始化回调方法
     *
     * <p><strong>初始化流程：</strong></p>
     * <ol>
     *   <li><strong>获取分布式锁：</strong>使用 Redisson 获取分布式锁，避免多实例并发初始化导致的节点编号冲突</li>
     *   <li><strong>分配节点编号：</strong>通过 Redis 自增键为当前实例分配唯一的节点编号</li>
     *   <li><strong>节点编号重置：</strong>当节点编号超过最大值时，重置为 0 并更新 Redis 中的计数</li>
     *   <li><strong>创建生成器：</strong>使用分配的节点编号创建 {@link DistributedIdGenerator} 实例</li>
     *   <li><strong>释放锁：</strong>无论初始化成功失败，都在 finally 块中释放分布式锁</li>
     * </ol>
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 分布式锁键名：用于保证初始化过程的互斥性
        String LOCK_KEY = "distributed_pay_id_generator_lock_key";
        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_KEY);
        lock.lock();
        
        try {
            // 从分布式缓存中获取 StringRedisTemplate 实例
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            
            // Redis 中存储节点计数的键名
            String DISTRIBUTED_ID_GENERATOR_KEY = "distributed_pay_id_generator_config";
            
            // 执行 Redis 自增操作，为当前实例分配节点编号
            // 使用 Optional 处理可能的 null 返回值，确保安全性
            long incremented = Optional.ofNullable(instance.opsForValue().increment(DISTRIBUTED_ID_GENERATOR_KEY))
                    .orElse(0L);
            
            // 最大节点数限制
            int NODE_MAX = 32;
            
            // 节点编号重置逻辑：超过最大值时重置为 0
            if (incremented > NODE_MAX) {
                incremented = 0;
                // 更新 Redis 中的计数为 0
                instance.opsForValue().set(DISTRIBUTED_ID_GENERATOR_KEY, "0");
            }
            
            // 使用分配的节点编号创建分布式 ID 生成器实例
            DISTRIBUTED_ID_GENERATOR = new DistributedIdGenerator(incremented);
            
            // 初始化成功日志（建议添加）
            // log.info("PayIdGeneratorManager initialized successfully with nodeId: {}", incremented);
        } finally {
            // 释放分布式锁，确保无论初始化成功失败都会释放锁
            lock.unlock();
        }
    }
}
