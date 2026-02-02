package org.openzjl.index12306.biz.payservice.dto.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款响应对象
 *
 * @author zhangjlk
 * @date 2026/1/28 19:19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class RefundResponse {

    /**
     * 退款状态
     */
    private Integer status;

    /**
     * 第三方交易凭证
     */
    private String tradeNo;


}
