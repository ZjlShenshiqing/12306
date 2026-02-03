/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.framework.starter.database.base.BaseDO;

/**
 * 用户名复用表实体
 *
 * @author zhangjlk
 * @date 2026/1/9 11:00
 */
@Data
@TableName("t_user_reuse")
@NoArgsConstructor
@AllArgsConstructor
public class UserReuseDO extends BaseDO {

    /**
     * 用户名
     */
    private String username;
}
