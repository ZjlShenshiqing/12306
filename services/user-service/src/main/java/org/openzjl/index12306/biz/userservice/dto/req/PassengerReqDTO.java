package org.openzjl.index12306.biz.userservice.dto.req;

import lombok.Data;

/**
 * 乘车人添加 & 修改请求参数
 *
 * @author zhangjlk
 * @date 2026/1/11 20:50
 */
@Data
public class PassengerReqDTO {

    /**
     * 乘车人ID
     */
    private String id;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private Integer idType;

    /**
     * 证件号
     */
    private String idCard;

    /**
     * 优惠类型
     */
    private Integer discountType;

    /**
     * 手机号
     */
    private String phone;
}
