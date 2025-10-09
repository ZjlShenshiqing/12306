package org.openzjl.index12306.framework.starter.idempotent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * 幂等属性配置 - token
 *
 * @author zhangjlk
 * @date 2025/10/6 10:53
 */
@Data
@ConfigurationProperties(prefix = IdempotentProperties.PREFIX)
public class IdempotentProperties {

    public static final String PREFIX = "framework.idempotent.token";

    /**
     * Token 幂等 key 前缀
     */
    private String prefix;

    /**
     * Token申请后过期时间
     * 单位默认毫秒 {@link TimeUnit#MICROSECONDS}
     */
    private Long timout;
}
