package org.openzjl.index12306.framework.starter.idempotent.config;

import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.idempotent.core.token.IdempotentTokenExecuteHandler;
import org.openzjl.index12306.framework.starter.idempotent.core.token.IdempotentTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 *
 * @author zhangjlk
 * @date 2025/10/6 10:53
 */
@EnableConfigurationProperties(IdempotentProperties.class)
public class IdempotentAutoConfiguration {

    public IdempotentTokenService idempotentTokenExecuteHandler(DistributedCache distributedCache,
                                                                IdempotentProperties idempotentProperties) {
        return new IdempotentTokenExecuteHandler(distributedCache, idempotentProperties);
    }
}
