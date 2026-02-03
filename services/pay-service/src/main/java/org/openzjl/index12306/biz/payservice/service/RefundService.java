/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.service;

import org.openzjl.index12306.biz.payservice.dto.req.RefundReqDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.RefundRespDTO;

/**
 * 退款接口层
 *
 * @author zhangjlk
 * @date 2026/1/27 12:22
 */
public interface RefundService {

    /**
     * 公共退款接口
     *
     * @param requestParam 退款请求参数
     * @return 退款返回详情
     */
    RefundRespDTO commonRefund(RefundReqDTO requestParam);
}
