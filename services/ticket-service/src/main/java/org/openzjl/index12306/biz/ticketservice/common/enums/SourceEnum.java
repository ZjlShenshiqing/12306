/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 购票来源
 *
 * @author zhangjlk
 * @date 2026/1/2 21:40
 */
@RequiredArgsConstructor
public enum SourceEnum {

    /**
     * 线上购票
     */
    INTERNET(0),

    /**
     * 窗口购票
     */
    OFFLINE(1);

    @Getter
    private final Integer code;
}
