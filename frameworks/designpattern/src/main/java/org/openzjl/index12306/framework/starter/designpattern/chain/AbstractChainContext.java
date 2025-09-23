package org.openzjl.index12306.framework.starter.designpattern.chain;

import org.openzjl.index12306.framework.starter.bases.ApplicationContextHolder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 抽象责任链上下文
 *
 * 这个类是一个 “责任链调度中心”，它的作用是：
 *
 * 启动时自动从 Spring 容器中找出所有实现了 AbstractChainHandler 的处理器；
 * 按照它们的 mark() 分类；
 * 在每个分类里按 order 排序；
 * 提供一个统一入口：handler(mark, requestParam)，让你可以“一键触发某条责任链”。
 *
 * @author zhangjlk
 * @date 2025/9/17 19:43
 */
// CommandLineRunner 是 Spring Boot 提供的一个接口，它的作用是：在项目启动完成后，自动执行一段代码
public final class AbstractChainContext<T> implements CommandLineRunner {

    // 自己定义的那些实现了 AbstractChainHandler 接口的 Bean 的集合
    private final Map<String, List<AbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    /**
     * 责任链执行
     * @param mark          责任链组件标识
     * @param requestParam  请求参数
     */
    public void handler(String mark, T requestParam) {
        List<AbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        if (CollectionUtils.isEmpty(abstractChainHandlers)) {
            throw new RuntimeException("No chain handler for mark: " + mark);
        }
        // 依次执行责任链里面的handler
        abstractChainHandlers.forEach(handler -> handler.handler(requestParam));
    }

    /**
     * 填充Bean集合
     */
    @Override
    public void run(String... args) throws Exception {
        // 拿到所有标注了bean的责任链执行类
        Map<String, AbstractChainHandler> chainFilterMap = ApplicationContextHolder
                .getBeansOfType(AbstractChainHandler.class);
        chainFilterMap.forEach((beanName, bean) -> {
            List<AbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(bean.mark());
            if (CollectionUtils.isEmpty(abstractChainHandlers)) {
                abstractChainHandlers = new ArrayList<>();
            }
            abstractChainHandlers.add(bean);
            List<AbstractChainHandler> actualAbstractChanHandlers = abstractChainHandlers.stream()
                    .sorted(Comparator.comparing(Ordered::getOrder))
                    .collect(Collectors.toList());
            abstractChainHandlerContainer.put(bean.mark(), actualAbstractChanHandlers);
        });
    }
}
