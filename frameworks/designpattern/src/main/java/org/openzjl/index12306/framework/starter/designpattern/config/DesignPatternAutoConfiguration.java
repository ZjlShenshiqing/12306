package org.openzjl.index12306.framework.starter.designpattern.config;

import org.openzjl.index12306.framework.starter.bases.config.ApplicationBaseAutoConfiguration;
import org.openzjl.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractStrategyChoose;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 设计模式自动装配
 * @author zhangjlk
 * @date 2025/9/17 19:43
 */
@ImportAutoConfiguration(ApplicationBaseAutoConfiguration.class) // 加载基础组件配置
public class DesignPatternAutoConfiguration {

    /**
     * 策略模式选择器
     */
    @Bean
    public AbstractStrategyChoose abstractStrategyChoose() {
        return new AbstractStrategyChoose();
    }

    /**
     * 责任链上下文
     */
    @Bean
    public AbstractChainContext abstractChainContext() {
        return new AbstractChainContext();
    }
}
