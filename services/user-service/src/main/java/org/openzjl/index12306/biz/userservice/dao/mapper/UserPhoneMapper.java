/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.openzjl.index12306.biz.userservice.dao.entity.UserMailDO;
import org.openzjl.index12306.biz.userservice.dao.entity.UserPhoneDO;

/**
 * 用户手机号持久层
 *
 * @author zhangjlk
 * @date 2026/1/8 16:35
 */
public interface UserPhoneMapper extends BaseMapper<UserPhoneDO> {

    /**
     * 注销用户
     *
     * @param userPhoneDO 注销用户入参
     */
    void deletionUser(UserPhoneDO userPhoneDO);
}
