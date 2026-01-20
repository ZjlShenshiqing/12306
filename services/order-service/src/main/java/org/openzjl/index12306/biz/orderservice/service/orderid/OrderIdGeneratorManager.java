package org.openzjl.index12306.biz.orderservice.service.orderid;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 订单ID全局唯一生成器管理器
 * 负责在应用启动时从Redis分配唯一的节点ID，并管理分布式ID生成器的实例
 * 使用Redis分布式锁保证多实例环境下节点ID分配的唯一性
 * 
 * 工作流程：
 * 1. 应用启动时，通过Redis原子操作获取并递增节点ID
 * 2. 使用分布式锁保证多实例环境下节点ID分配的唯一性
 * 3. 节点ID范围：0-31（共32个节点）
 * 4. 当节点ID超过最大值时，重置为0（循环使用）
 *
 * @author zhangjlk
 * @date 2026/1/16 22:26
 */
@Component
@RequiredArgsConstructor
public final class OrderIdGeneratorManager implements InitializingBean {

    /**
     * Redisson客户端，用于获取分布式锁
     * 注意：当前未通过构造函数注入，可能需要手动注入或使用@Autowired
     */
    private RedissonClient redissonClient;

    /**
     * 分布式缓存实例，用于访问Redis
     */
    private final DistributedCache distributedCache;

    /**
     * 分布式ID生成器静态实例
     * 在应用启动时初始化，后续所有订单ID生成都使用此实例
     */
    private static DistributedIdGenerator DISTRIBUTED_ID_GENERATOR;

    /**
     * 生成订单全局唯一ID
     * 组合方式：分布式ID生成器生成的ID + 用户ID的后6位
     * 
     * 例如：
     * - 分布式ID生成器生成：1234567890123456
     * - 用户ID：1234567890
     * - 用户ID % 1000000 = 567890
     * - 最终订单ID：1234567890123456567890
     *
     * @param userId 用户ID
     * @return 订单ID
     */
    public static String generateId(long userId) {
        // 分布式ID生成器生成基础ID + 用户ID的后6位（取模1000000）
        // 这样可以在订单ID中包含用户信息，便于后续查询和统计
        return DISTRIBUTED_ID_GENERATOR.generateId() + String.valueOf(userId % 1000000);
    }

    /**
     * Spring Bean初始化后执行的方法
     * 在应用启动时自动调用，用于从Redis分配节点ID并初始化分布式ID生成器
     * 使用分布式锁保证多实例环境下节点ID分配的唯一性
     *
     * @throws Exception 初始化过程中的异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 分布式锁的键，用于保证多实例环境下节点ID分配的唯一性
        String LOCK_KEY = "distributed_id_generator_lock_key";
        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_KEY);
        // 加锁，保证同一时刻只有一个实例在分配节点ID
        lock.lock();
        try {
            // 获取Redis操作实例
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            // Redis中存储节点ID计数器的键
            String DISTRIBUTED_ID_GENERATOR_KEY = "distributed_id_generator_config";
            // 使用Redis的原子递增操作获取节点ID
            // 每次调用increment都会返回递增后的值，保证节点ID的唯一性
            long incremented = Optional.ofNullable(instance.opsForValue().increment(DISTRIBUTED_ID_GENERATOR_KEY)).orElse(0L);
            // 节点ID的最大值（32个节点，对应NODE_BITS=5，可表示0-31）
            int NODE_MAX = 32;
            // 如果节点ID超过最大值，重置为0（循环使用节点ID）
            if (incremented > NODE_MAX) {
                incremented = 0;
                // 重置Redis中的计数器为0
                instance.opsForValue().set(DISTRIBUTED_ID_GENERATOR_KEY, "0");
            }
            // 使用分配到的节点ID创建分布式ID生成器实例
            DISTRIBUTED_ID_GENERATOR = new DistributedIdGenerator(incremented);
        } finally {
            // 释放分布式锁，确保锁一定会被释放
            lock.unlock();
        }
    }
}
