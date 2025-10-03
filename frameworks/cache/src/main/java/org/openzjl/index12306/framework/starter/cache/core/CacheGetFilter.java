package org.openzjl.index12306.framework.starter.cache.core;

/**
 * 缓存过滤
 *
 * @author zhangjlk
 * @date 2025/10/2 17:03
 */
@FunctionalInterface
public interface CacheGetFilter<T> {

    /**
     * 对缓存值进行过滤判断。
     *
     * <p>
     * 该方法在缓存命中后被调用，传入缓存中取出的对象。
     * 返回 {@code true} 表示该缓存值有效，可直接返回；
     * 返回 {@code false} 表示该缓存值无效，应视为“缓存未命中”，
     * 通常会触发 {@link CacheLoader#load()} 重新加载数据。
     * </p>
     *
     * @param param 从缓存中获取到的值（可能为 null，取决于缓存实现）
     * @return {@code true} 表示缓存有效，{@code false} 表示缓存无效需重新加载
     */
    boolean filter(T param);
}
