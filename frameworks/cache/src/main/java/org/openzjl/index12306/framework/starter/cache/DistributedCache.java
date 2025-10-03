package org.openzjl.index12306.framework.starter.cache;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.openzjl.index12306.framework.starter.cache.core.CacheGetFilter;
import org.openzjl.index12306.framework.starter.cache.core.CacheGetIfAbsent;
import org.openzjl.index12306.framework.starter.cache.core.CacheLoader;
import org.redisson.api.RBloomFilter;

import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存操作统一接口
 *
 * @author zhangjlk
 * @date 2025/10/2 17:01
 */
public interface DistributedCache extends Cache {

    /**
     * 获取缓存，若查询结果为空，调用CacheLoader加载缓存
     */
    <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout);

    /**
     * 获取缓存，若查询结果为空，调用CacheLoader加载缓存
     */
    <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit unit);

    /**
     * 以一种安全的方式获取缓存，若查询结果为空，调用CacheLoader加载缓存
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，适用于不被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout);

    /**
     * 以一种安全的方式获取缓存，若查询结果为空，调用CacheLoader加载缓存
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，适用于不被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit unit);

    /**
     * 以一种安全的方式获取缓存，若查询结果为空，调用CacheLoader加载缓存
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter);

    /**
     * 以一种安全的方式获取缓存，若查询结果为空，调用CacheLoader加载缓存
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter);

    /**
     * 以一种安全的方式获取缓存，若查询结果为空，调用CacheLoader加载缓存
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，需要客户端传递布隆过滤器，并通过CacheFilter解决布隆过滤器无法删除的问题，适用于被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter);

    /**
     * 以一种安全的方式获取缓存，若查询结果为空，调用CacheLoader加载缓存
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，需要客户端传递布隆过滤器，并通过CacheFilter解决布隆过滤器无法删除的问题，适用于被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter, CacheGetIfAbsent<String> cacheGetIfAbsent);

    /**
     * 以一种安全的方式获取缓存，若查询结果为空，调用CacheLoader加载缓存
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，需要客户端传递布隆过滤器，并通过CacheFilter解决布隆过滤器无法删除的问题，适用于被外部直接调用的接口
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter, CacheGetIfAbsent<String> cacheGetIfAbsent);

    /**
     * 载入缓存，自定义超时时间
     */
    void put(@NotBlank String key, Object value, long timeout);

    /**
     * 载入缓存，自定义超时时间
     */
    void put(@NotBlank String key, Object value, long timeout, TimeUnit unit);

    /**
     * 载入缓存，自定义超时时间
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     */
    void safePut(@NotBlank String key, Object value, long timeout, RBloomFilter<String> bloomFilter);

    /**
     * 载入缓存，自定义超时时间
     * 通过此方式以防止程序出现：缓存击穿、缓存雪崩等问题，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     */
    void safePut(@NotBlank String key, Object value, long timeout, TimeUnit unit, RBloomFilter<String> bloomFilter);

    /**
     * 统计指定key的存在数量
     */
    Long countExistingKeys(@NotNull String... keys);
}
