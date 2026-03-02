/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.dto.handler.base;

import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.biz.payservice.dto.base.PayResponse;

/**
 * 抽象支付组件
 *
 * @author zhangjlk
 * @date 2026/2/28
 */
public abstract class AbstractPayHandler {

    /**
     * 支付抽象接口
     *
     * @param payRequest 支付请求参数
     * @return 支付响应参数
     */
    public abstract PayResponse pay(PayRequest payRequest);
}
