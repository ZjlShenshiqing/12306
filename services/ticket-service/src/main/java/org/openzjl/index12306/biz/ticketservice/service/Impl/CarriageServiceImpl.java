/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.dao.entity.CarriageDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.CarriageMapper;
import org.openzjl.index12306.biz.ticketservice.service.CarriageService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.cache.core.CacheLoader;
import org.openzjl.index12306.framework.starter.cache.toolkit.CacheUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_CARRIAGE_NUMBER_LIST;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_CARRIAGE;

/**
 * 列车车厢接口层实现
 *
 * @author zhangjlk
 * @date 2025/12/1 10:26
 */
@Service
@RequiredArgsConstructor
public class CarriageServiceImpl implements CarriageService {

    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final CarriageMapper carriageMapper;

    @Override
    public List<String> listCarriageNumber(String trainId, Integer carriageType) {

        // 拼接 Redis Key
        final String key = TRAIN_CARRIAGE + trainId;

        // 调用"防缓存击穿"的安全获取方法
        // safeGetCarriageNumber 负责处理：查缓存 -> 加锁 -> 双重检查 -> 兜底查库 -> 回写缓存
        return safeGetCarriageNumber(
                trainId,       // 用于生成分布式锁的 key
                key,           // Redis 缓存的大 Key
                carriageType,  // 对应 Hash 结构里的小 Key (Field)

                // 3. 定义 CacheLoader (回调逻辑)
                // 这部分代码只有在"缓存没命中"且"抢到了锁"的时候才会执行！
                () -> {
                    // --- MyBatis Plus 查询构建 ---
                    LambdaQueryWrapper<CarriageDO> queryWrapper = Wrappers.lambdaQuery(CarriageDO.class)
                            .eq(CarriageDO::getTrainId, trainId)       // SQL: WHERE train_id = 'G1024'
                            .eq(CarriageDO::getCarriageType, carriageType); // SQL: AND carriage_type = 1

                    // --- 执行数据库查询 ---
                    // carriageDOList 里存的是完整的数据库实体对象
                    List<CarriageDO> carriageDOList = carriageMapper.selectList(queryWrapper);

                    // 我们只需要"车厢号"这一列，不需要整个对象
                    List<String> carriageListWithOnlyOneNumber = carriageDOList.stream()
                            .map(CarriageDO::getCarriageNumber) // 提取 carriageNumber 字段
                            .collect(Collectors.toList());

                    // --- 序列化为字符串 ---
                    // 因为我们的 Redis 结构约定 Value 是 String，所以要把 List 转成逗号分隔的字符串
                    // 例如: List ["1", "5"] -> String "1,5"
                    // safeGetCarriageNumber 拿到这个字符串存入 Redis 后，会再把它 split 回 List 返回给前端
                    return StrUtil.join(StrUtil.COMMA, carriageListWithOnlyOneNumber);
                }
        );
    }


    /**
     * 线程安全地获取车厢号列表 (防缓存击穿版)
     * <p>
     * 流程：查缓存 -> 没查到 -> 加锁 -> (再查缓存 -> 还没有 -> 查库并回写) -> 解锁
     */
    private List<String> safeGetCarriageNumber(String trainId, final String key, Integer carriageType, CacheLoader<String> cacheLoader) {

        // ==========================================
        // 第一步：快速检查
        // ==========================================
        // 先尝试直接从 Redis 拿数据，绝大多数流量会在这里直接返回，性能最高。
        String result = getCarriageNumber(trainId, carriageType);

        // 如果缓存里有数据 (Result is Not Null)，直接分割字符串并返回
        // 假设缓存存的是 "1,2,3,4"，这里分割成 List ["1", "2", "3", "4"]
        if (!CacheUtil.isNullOrBlank(result)) {
            return StrUtil.split(result, StrUtil.COMMA);
        }

        // ==========================================
        // 第二步：缓存未命中，准备回源
        // ==========================================
        // 此时缓存没数据，需要去数据库查。
        // 为了防止 1000 个线程同时去查数据库，我们需要一把锁。
        // 锁的粒度是 trainId，说明锁的是"这趟车"，不影响查"别的车"。
        RLock lock = redissonClient.getLock(String.format(LOCK_QUERY_CARRIAGE_NUMBER_LIST, trainId));

        lock.lock(); // 加锁！后面的代码同一时间只有一个线程能执行
        try {
            // ==========================================
            // 第三步：双重检查 - 核心逻辑
            // ==========================================
            // 为什么要再查一次？
            // 因为在"当前线程"排队等待获取锁的过程中，可能"前一个获得锁的线程"已经把数据查出来并写入 Redis 了。
            // 如果这里不检查，就会导致当前线程重复去查数据库，浪费性能。
            if (CacheUtil.isNullOrBlank(result = getCarriageNumber(trainId, carriageType))) {

                // ==========================================
                // 第四步：真的没有数据，查库并回写 (Load and Set)
                // ==========================================
                // 调用之前定义的 loadAndSet 方法：
                // 1. 执行 cacheLoader.load() 查 DB
                // 2. 将结果写入 Redis (Set)
                // 3. 返回结果
                if (CacheUtil.isNullOrBlank(result = loadAndSet(carriageType, key, cacheLoader))) {
                    // 如果数据库里也没查到，为了防止空指针，返回空列表
                    return Collections.emptyList();
                }
            }
        } finally {
            // ==========================================
            // 第五步：兜底解锁
            // ==========================================
            lock.unlock();
        }

        // 将最终拿到的结果 (无论是刚查出来的，还是第二次缓存检查拿到的) 处理并返回
        return StrUtil.split(result, StrUtil.COMMA);
    }

    /**
     * 根据 Redis Key 和 车厢类型(carriageType) 查询具体的车厢编号。
     * <p>
     * 对应 Redis 命令: HGET key field
     * 例如: HGET train:G1024:carriages 1
     * 返回值: "ZE123456" (或者是 null)
     */
    private String getCarriageNumber(final String key, Integer carriageType) {
        /**
         * // 方法签名：
         * HashOperations<String, Object, Object>
         *                  ↑       ↑       ↑
         *                  |       |       |
         *       (1) Redis的大Key   |       |
         *                          |       |
         *               (2) Hash里面的小Key (Field)
         *                                  |
         *                         (3) Hash里面的具体值 (Value)
         */
        HashOperations<String, Object, Object> hashOperations = getHashOperations();
        // 执行查询，并使用 Optional 包装结果以防止空指针异常 (NPE)
        // hashOperations.get(大Key, 小Key)
        // 注意：这里将 carriageType (Integer) 转成了 String 作为 Hash 的 Field
        return Optional.ofNullable(hashOperations.get(key, String.valueOf(carriageType)))

                // 如果上一步拿到的值不是 null，执行这个转换，这里将 Redis 返回的 Object 强转为 String
                .map(Object::toString)

                // .orElse(): 如果上一步拿到的是 null，则返回一个默认的空字符串 ""，保证方法永远不会返回 null
                .orElse("");
    }

    /**
     * 获取 Redis Hash 操作模版。
     * <p>
     * 用于执行 Redis 的 Hash 相关命令 (如 HSET, HGET, HDEL 等)。
     * 通过此方法获取的操作对象，可以直接对 Redis 中的 Hash 结构进行读写。
     *
     * @return HashOperations<Key类型, Field类型, Value类型> 返回 Hash 操作句柄
     * @throws ClassCastException 如果 distributedCache 底层实例不是 StringRedisTemplate 类型
     */
    private HashOperations<String, Object, Object> getHashOperations() {
        // 从缓存包装类中获取 StringRedisTemplate 实例
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 返回 Hash 类型的操作接口
        return stringRedisTemplate.opsForHash();
    }

    /**
     * 核心功能：加载数据并回填到缓存 (Load and Set)
     * <p>
     * 场景：通常用于缓存击穿后的"亡羊补牢"。
     * 当 getCarriageNumber 发现 Redis 里没数据时，调用此方法去数据库查，
     * 查到了就塞回 Redis，方便下一次直接读取。
     *
     * @param carriageType 车厢类型 (作为 Redis Hash 里的 Field/Key)
     * @param key          Redis 的大 Key (Hash 表的名字)
     * @param loader       函数式接口 (回调)，封装了"去数据库查询具体数据"的逻辑
     * @return String      返回查到的数据
     */
    private String loadAndSet(Integer carriageType, final String key, CacheLoader<String> loader) {

        // 回源查询 (Load)
        // 利用传入的 loader 回调，执行真正的数据库/RPC 查询逻辑
        String result = loader.load();

        // 2空值校验 (防缓存穿透/脏数据)
        // 如果数据库里也没查到 (null 或 空字符串)，就不往 Redis 里写了，直接返回。
        if (CacheUtil.isNullOrBlank(result)) {
            return result;
        }

        // 获取 Redis Hash 操作句柄 (就是我们之前分析的那个方法)
        HashOperations<String, Object, Object> hashOperations = getHashOperations();

        // 双重保障写入 (Set)，只有当 Redis 里真的不存在这个 key 时，才写入。
        // 作用：防止并发情况下，其他线程已经抢先一步把数据写入缓存了，这里就不需要重复覆盖了。
        hashOperations.putIfAbsent(key, String.valueOf(carriageType), result);

        // 返回结果给上层调用者
        return result;
    }
}
