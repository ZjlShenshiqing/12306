package org.openzjl.index12306.framework.starter.idempotent.core.token;

import org.openzjl.index12306.framework.starter.idempotent.core.IdempotentExecuteHandler;

/**
 * Token实现幂等 - 接口
 *
 * @author zhangjlk
 * @date 2025/10/6 15:37
 */
public interface IdempotentTokenService extends IdempotentExecuteHandler {

    /**
     * 创建幂等验证Token
     */
    String createToken();
}
