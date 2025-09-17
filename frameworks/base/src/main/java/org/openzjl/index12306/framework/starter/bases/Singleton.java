package org.openzjl.index12306.framework.starter.bases;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 单例对象容器
 * @author zhangjlk
 * @date 2025/9/6 21:51
 */
// 控制这个无参构造函数的访问权限
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Singleton {

    private static final ConcurrentHashMap<String, Object> SINGLE_OBJECT_POOL = new ConcurrentHashMap<>();

    /**
     * 根据key获取单例对象
     * @param key 单例对象主键
     * @return 单例对象
     * @param <T> 单例对象属性
     */
    public static <T> T get(String key) {
        Object result = SINGLE_OBJECT_POOL.get(key);
        return result == null ? null : (T) result;
    }

    /**
     * 对象放入容器
     * @param value 值
     */
    public static void put(Object value) {
        put(value.getClass().getName(), value);
    }

    public static void put(String key, Object value) {
        SINGLE_OBJECT_POOL.put(key, value);
    }
}
