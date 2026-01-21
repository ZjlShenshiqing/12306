package org.openzjl.index12306.biz.orderservice.dto.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单状态反转实体
 *
 * @author zhangjlk
 * @date 2026/1/21 15:05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusReversalDTO {

    /**
     * 订单号
     */
    private String orderSn;

    /**
     * 订单反转后状态
     */
    private Integer orderStatus;

    /**
     * 订单明细反转后状态
     */
    private Integer orderItemStatus;
}
