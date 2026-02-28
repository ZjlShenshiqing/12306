package org.openzjl.index12306.biz.payservice.dto.handler.base;

import com.alipay.api.AlipayApiException;
import lombok.SneakyThrows;
import org.openzjl.index12306.biz.payservice.dto.base.PayCallbackRequest;
import org.openzjl.index12306.biz.payservice.dto.base.RefundRequest;
import org.openzjl.index12306.biz.payservice.dto.base.RefundResponse;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * 抽象支付回调组件
 *
 * @author zhangjlk
 * @date 2026/2/28 上午11:50
 */
public abstract class AbstractRefundHandler {

    /**
     * 支付回调抽象接口
     *
     * @param payRequest 退款请求参数
     * @return 退款响应参数
     */
    public abstract RefundResponse refund(RefundRequest payRequest);
}
