package org.openzjl.index12306.biz.ticketservice.enums;

/**
 * 购票相关责任链枚举
 *
 * @author zhangjlk
 * @date 2025/12/24 下午8:38
 */
public enum TicketChainMarkEnum {

    /**
     * 车票查询过滤器
     */
    TRAIN_QUERY_FILTER,

    /**
     * 车票购买过滤器
     */
    TRAIN_PURCHASE_TICKET_FILTER,

    /**
     * 车票退款过滤器
     */
    TRAIN_REFUND_TICKET_FILTER
}
