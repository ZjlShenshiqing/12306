package org.openzjl.index12306.biz.payservice.dto.handler.base;

import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackRequest;

/**
 * 抽象支付回调组件
 *
 * @author zhangjlk
 * @date 2026/2/28 上午11:45
 */
public abstract class AbstractPayCallbackHandler {

    /**
     * 支付回调抽象接口
     *
     * @param payCallbackRequest 支付回调请求参数
     */
    public abstract void callback(PayCallbackRequest payCallbackRequest);
}
