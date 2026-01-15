package org.openzjl.index12306.biz.orderservice.dto.resp;

import cn.crane4j.annotation.AssembleEnum;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import org.openzjl.index12306.biz.orderservice.serialize.IdCardDesensitizationSerializer;

/**
 * 乘车人车票订单详情返回参数
 *
 * @author zhangjlk
 * @date 2026/1/14 11:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketOrderPassengerDetailRespDTO {

    /**
     * 乘车人ID
     */
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 座位类型
     */
    private Integer seatType;

    /**
     * 车厢号
     */
    private String carriageNumber;

    /**
     * 座位号
     */
    private String seatNumber;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private String idType;

    /**
     * 证件号
     */
    @JsonSerialize(using = IdCardDesensitizationSerializer.class)
    private String idCard;

    /**
     * 车票类型
     */
    private Integer ticketType;

    /**
     * 订单金额
     */
    private Integer amount;

    /**
     * 车票状态
     *
     * @AssembleEnum
     * 这是关键：一个“枚举装配/字典翻译”注解。
     *
     * 它的目的通常是：
     * 数据库/接口里存的是数字（Integer 的状态码），但前端/调用方更想要一个可读的中文/英文状态名。
     * 所以在返回结果时，框架会自动帮你把 status=1 翻译成 statusName="已支付" 这种“人能看懂”的文本。
     */
    @AssembleEnum(type = OrderItemStatusEnum.class, ref = "statusName")
    private Integer status;

    /**
     * 车票状态名称
     */

    private String statusName;
}
