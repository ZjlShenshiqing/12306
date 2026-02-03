/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.dto.req;

import lombok.Data;

/**
 * 用户修改请求参数
 *
 * @author zhangjlk
 * @date 2026/1/11 14:38
 */
@Data
public class UserUpdateReqDTO {

    /**
     * 用户id
     */
    private String id;

    /**
     * 姓名
     */
    private String username;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 旅客类型
     */
    private Integer userType;

    /**
     * 邮编
     */
    private String postCode;

    /**
     * 地址
     */
    private String address;
}
