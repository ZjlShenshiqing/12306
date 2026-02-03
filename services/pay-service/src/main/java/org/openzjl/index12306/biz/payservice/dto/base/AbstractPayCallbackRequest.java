/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.base;

import lombok.Getter;
import lombok.Setter;
import org.openzjl.index12306.biz.payservice.dto.req.AliPayCallbackRequest;

/**
 * 抽象支付回调入参实体
 *
 * @author zhangjlk
 * @date 2026/1/26 12:14
 */
public abstract class AbstractPayCallbackRequest implements PayCallbackRequest {

    @Getter
    @Setter
    private String orderRequestId;

    @Override
    public AliPayCallbackRequest getAliPayCallbackRequest() {
        return null;
    }

    @Override
    public String buildMark() {
        return "";
    }
}
