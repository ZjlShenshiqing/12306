/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.enums;

/**
 * 幂等验证类型枚举
 *
 * @author zhangjlk
 * @date 2025/10/5 18:49
 */
public enum IdempotentTypeEnum {

    /**
     * 基于TOKEN的方式进行验证
     */
    TOKEN,

    /**
     * 基于方法参数方式进行验证
     *
     * 把方法的参数（如订单ID、用户ID）拼接成一个唯一标识，作为幂等 Key 去 Redis 中判断是否已处理过
     */
    PARAM,

    /**
     * 基于SPEL表达式方式进行验证
     */
    SPEL
}
