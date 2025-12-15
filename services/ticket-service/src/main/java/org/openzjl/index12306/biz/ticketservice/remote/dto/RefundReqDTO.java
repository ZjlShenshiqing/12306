package org.openzjl.index12306.biz.ticketservice.remote.dto;

import lombok.Data;

/**
 * 退款请求入参实体
 *
 * @author zhangjlk
 * @date 2025/12/15 上午10:24
 */
@Data
public class RefundReqDTO {

    /**
     * 订单号
     */
    private String orderSn;


}
