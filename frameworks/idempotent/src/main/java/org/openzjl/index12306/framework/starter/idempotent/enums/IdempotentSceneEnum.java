/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.enums;

/**
 * 幂等验证场景枚举
 *
 * @author zhangjlk
 * @date 2025/10/5 18:49
 */
public enum IdempotentSceneEnum {

    /**
     * 基于 RestApi 的方式进行验证
     */
    RESTAPI,

    /**
     * 基于 MQ 的方式进行验证
     */
    MQ
}
