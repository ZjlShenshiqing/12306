package org.openzjl.index12306.biz.ticketservice.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消车票订单请求入参
 *
 * @author zhangjlk
 * @date 2025/12/13 下午3:44
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelTicketOrderReqDTO {

    /**
     * 订单号
     */
    private String orderSn;
}
