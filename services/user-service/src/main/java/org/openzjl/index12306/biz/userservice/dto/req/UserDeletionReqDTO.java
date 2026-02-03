/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.dto.req;

import lombok.Data;

/**
 * 用户注销请求参数
 *
 * @author zhangjlk
 * @date 2026/1/9 11:38
 */
@Data
public class UserDeletionReqDTO {

    /**
     * 用户名
     */
    private String username;
}
