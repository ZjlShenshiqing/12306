package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto;

import lombok.Data;

/**
 * 列车购票出参
 *
 * @author zhangjlk
 * @date 2025/12/5 上午10:02
 */
@Data
public class TrainPurchaseTicketRespDTO {

    /**
     * 乘车人ID
     */
    private String passengerId;

    /**
     * 乘车人真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private Integer idCard;

    /**
     * 乘车人手机号
     */
    private String phone;

    /**
     * 车票/用户类型 0：成人 1：儿童 2：学生 3：残疾军人
     */
    private Integer userType;

    /**
     * 车厢号
     */
    private String carriageNumber;

    /**
     * 座位号
     */
    private String seatNumber;

    /**
     * 座位金额
     */
    private Integer amount;
}
