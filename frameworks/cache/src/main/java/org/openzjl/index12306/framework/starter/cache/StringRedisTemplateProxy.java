package org.openzjl.index12306.framework.starter.cache;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.framework.starter.bases.Singleton;
import org.openzjl.index12306.framework.starter.cache.config.RedisDistributedProperties;
import org.openzjl.index12306.framework.starter.cache.core.CacheGetFilter;
import org.openzjl.index12306.framework.starter.cache.core.CacheGetIfAbsent;
import org.openzjl.index12306.framework.starter.cache.core.CacheLoader;
import org.openzjl.index12306.framework.starter.cache.toolkit.CacheUtil;
import org.openzjl.index12306.framework.starter.cache.toolkit.FastJson2Util;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.sql.Time;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存：操作Redis模版代理
 *
 * @author zhangjlk
 * @date 2025/10/2 17:02
 */
@RequiredArgsConstructor
public class StringRedisTemplateProxy implements DistributedCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisDistributedProperties redisProperties;
    private final RedissonClient redissonClient;

    private static final String LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH = "lua/putIfAllAbsent.lua";
    private static final String SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX = "safe_get_distributed_lock_get";

    @Override
    public <T> T get(String key, Class<T> clazz) {
        String value = stringRedisTemplate.opsForValue().get(key);
        // 如果目标类型 T 是 String，就直接返回原始的 value（假设它已经是字符串）
        if (String.class.isAssignableFrom(clazz)) {
            return (T) value;
        }
        // 对JSON进行反序列化
        return JSON.parseObject(value, FastJson2Util.buildType(clazz));
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, redisProperties.getValueTimeout());
    }

    @Override
    public Boolean putIfAllAbsent(Collection<String> keys) {
        // 通过一个单例工具类（Singleton.get）懒加载并复用一个 Redis Lua 脚本对象（DefaultRedisScript<Boolean>），避免重复创建和解析脚本
        DefaultRedisScript<Boolean> actual = Singleton.get(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH, () -> {
            // 创建 Redis Lua 脚本封装对象
            DefaultRedisScript defaultRedisScript = new DefaultRedisScript<>();

            // 指定脚本源：从 classpath 下的指定路径加载 Lua 文件
            defaultRedisScript.setScriptSource(
                    new ResourceScriptSource(
                            new ClassPathResource(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH)
                    )
            );

            // 声明该脚本执行后返回的结果类型为 Boolean
            defaultRedisScript.setResultType(Boolean.class);

            // 返回初始化完成的脚本对象，供 Singleton 缓存并返回
            return defaultRedisScript;
        });
        Boolean result = stringRedisTemplate.execute(actual, Lists.newArrayList(keys), redisProperties.getValueTimeout().toString());
        return result != null && result;
    }

    @Override
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    @Override
    public Long delete(Collection<String> keys) {
        return stringRedisTemplate.delete(keys);
    }

    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public Object getInstance() {
        return stringRedisTemplate;
    }

    @Override
    public <T> T get(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return get(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit());
    }

    @Override
    public <T> T get(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit unit) {
        T result = get(key, clazz);
        if (!CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        return loadAndSet(key, cacheLoader, timeout, unit, false, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit());
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit unit) {
        return safeGet(key, clazz, cacheLoader, timeout, unit, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, bloomFilter, null, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, bloomFilter, null, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader,
                         long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter) {
        return safeGet(key, clazz, cacheLoader, timeout, timeUnit, bloomFilter, null, null);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader,
                         long timeout, RBloomFilter<String> bloomFilter,
                         CacheGetFilter<String> cacheCheckFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {
        return safeGet(key, clazz, cacheLoader, timeout, redisProperties.getValueTimeUnit(), bloomFilter, cacheCheckFilter, cacheGetIfAbsent);
    }

    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader,
                         long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter,
                         CacheGetFilter<String> cacheGetFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {

        // 1. 尝试从缓存中直接获取数据
        T result = get(key, clazz);

        // 2. 判断是否可以直接返回缓存结果，满足任一条件即返回：
        //    a) 缓存值非空（非 null 且非空字符串）
        //    b) cacheGetFilter 存在且对当前 key 返回 true（表示即使缓存为空也应返回）
        //    c) bloomFilter 存在且确认 key 不存在（说明数据肯定不存在，无需回源）
        if (!CacheUtil.isNullOrBlank(result)
                || Optional.ofNullable(cacheGetFilter).map(each -> each.filter(key)).orElse(false)
                || Optional.ofNullable(bloomFilter).map(each -> !each.contains(key)).orElse(false)) {
            return result;
        }

        // 3. 缓存未命中且不能提前返回，尝试获取分布式锁（防止缓存击穿）
        RLock lock = redissonClient.getLock(SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX + key);
        lock.lock();
        try {
            // 4. 双重检查：再次尝试从缓存获取（避免多个线程重复加载）
            if (CacheUtil.isNullOrBlank(result = get(key, clazz))) {
                // 5. 缓存仍为空，触发回源加载并写入缓存
                if (CacheUtil.isNullOrBlank(result = loadAndSet(key, cacheLoader, timeout, timeUnit, true, bloomFilter))) {
                    // 6. 若加载结果仍为空，执行“缓存未命中后置操作”（如记录日志、返回兜底值等）
                    Optional.ofNullable(cacheGetIfAbsent).ifPresent(each -> each.execute(key));
                }
            }

        } finally {
            lock.unlock();
        }
        // 返回最终结果（可能为有效值、null 或空字符串）
        return result;
    }

    @Override
    public void put(String key, Object value, long timeout) {
        put(key, value, timeout, redisProperties.getValueTimeUnit());
    }

    @Override
    public void put(String key, Object value, long timeout, TimeUnit unit) {
        String actual = value instanceof String ? (String) value : JSON.toJSONString(value);
        stringRedisTemplate.opsForValue().set(key, actual, timeout, unit);
    }

    @Override
    public void safePut(String key, Object value, long timeout, RBloomFilter<String> bloomFilter) {
        safePut(key, value, timeout, redisProperties.getValueTimeUnit(), bloomFilter);
    }

    @Override
    public void safePut(String key, Object value, long timeout, TimeUnit unit, RBloomFilter<String> bloomFilter) {
        put(key, value, timeout, unit);
        // 如果布隆过滤器（bloomFilter）不为 null，则将当前 key 添加到布隆过滤器中
        if (bloomFilter != null) {
            bloomFilter.add(key);
        }
    }

    @Override
    public Long countExistingKeys(@NotNull String... keys) {
        return stringRedisTemplate.countExistingKeys(Lists.newArrayList(keys));
    }

    private <T> T loadAndSet(String key, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, boolean safeFlag, RBloomFilter<String> bloomFilter) {
        T result = cacheLoader.load();
        if (CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        if (safeFlag) {
            safePut(key, result, timeout, timeUnit, bloomFilter);
        } else {
            put(key, result, timeout, timeUnit);
        }

        return result;
    }
}
