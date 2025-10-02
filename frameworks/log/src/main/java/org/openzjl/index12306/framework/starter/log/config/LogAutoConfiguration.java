package org.openzjl.index12306.framework.starter.log.config;

import org.openzjl.index12306.framework.starter.log.core.ILogPrintAspect;
import org.springframework.context.annotation.Bean;

/**
 * 日志自动装配
 *
 * @author zhangjlk
 * @date 2025/9/30 20:42
 */
public class LogAutoConfiguration {

    @Bean
    public ILogPrintAspect iLogPrintAspect() {
        return new ILogPrintAspect();
    };
}
