/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.cache.core;

/**
 * 缓存查询为空
 *
 * @author zhangjlk
 * @date 2025/10/2 17:03
 */
@FunctionalInterface
public interface CacheGetIfAbsent<T> {

    /**
     * 如果缓存的查询结果为空，执行该逻辑
     */
    void execute(T param);
}
