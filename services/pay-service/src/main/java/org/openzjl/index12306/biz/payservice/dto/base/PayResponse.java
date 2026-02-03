/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付返回
 *
 * @author zhangjlk
 * @date 2026/1/23 17:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class PayResponse {

    /**
     * 调用支付返回信息
     */
    private String body;
}
