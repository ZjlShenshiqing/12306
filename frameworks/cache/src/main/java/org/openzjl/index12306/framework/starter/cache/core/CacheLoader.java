package org.openzjl.index12306.framework.starter.cache.core;

/**
 * 缓存加载器
 *
 * <p>
 * 该接口定义了一个“延迟加载缓存值”的通用契约。
 * 当缓存中没有所需数据（缓存未命中）时，系统可以调用此接口的 {@link #load()} 方法，
 * 从原始数据源（如数据库、远程 API、文件等）加载数据，并将其放入缓存。
 * </p>
 *
 * <h3>设计目的</h3>
 * <ul>
 *   <li>解耦缓存逻辑与数据加载逻辑：缓存组件不需要知道数据具体怎么来的，只需调用 {@code load()}。</li>
 *   <li>支持懒加载（Lazy Loading）：只有在真正需要时才去加载数据，避免资源浪费。</li>
 *   <li>便于测试和替换：可通过传入不同的实现来模拟或切换数据源。</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 从数据库加载用户信息
 * CacheLoader<User> userLoader = () -> userRepository.findById(userId);
 *
 * // 在缓存工具类中使用
 * User user = cache.get("user:" + userId, userLoader);
 * </pre>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:03
 */
@FunctionalInterface // 表明这是一个函数式接口（仅含一个抽象方法），可使用 Lambda 表达式
public interface CacheLoader<T> {

    /**
     * 执行实际的数据加载操作，并返回加载结果
     *
     * <p>
     * 此方法通常会被缓存框架在以下情况调用：
     * <ul>
     *   <li>缓存中不存在指定 key 的数据（缓存穿透）</li>
     *   <li>缓存已过期，需要重新加载</li>
     * </ul>
     * </p>
     *
     * @return 加载得到的数据对象，不能为 {@code null}（除非缓存允许存 null）
     * @throws Exception 如果加载过程中发生错误（如数据库连接失败、网络超时等）
     */
    T load();
}
