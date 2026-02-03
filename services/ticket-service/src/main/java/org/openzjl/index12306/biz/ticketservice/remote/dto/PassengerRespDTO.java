/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.remote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 乘车人返回参数
 *
 * @author zhangjlk
 * @date 2025/12/31 上午10:13
 */
@Data
public class PassengerRespDTO {

    /**
     * 乘车人ID
     */
    private String id;

    /**
     * 用户名
     */
    private String userName;

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
     * 0：成人
     * 1：儿童
     * 2：学生
     * 3：残疾军人
     */
    private Integer discountType;
    /**
     * 手机号
     */
    private String phone;

    /**
     * 添加日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createDate;

    /**
     * 核验状态
     */
    private Integer verifyStatus;
}
