/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.service.handler.filter.user;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import org.openzjl.index12306.biz.userservice.service.UserService;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

/**
 * 用户注册检查证件号是否多次注销
 *
 * @author zhangjlk
 * @date 2026/1/13 18:05
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterCheckDeletionChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final UserService userService;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        Integer userDeletionNum = userService.queryUserDeletionNum(requestParam.getIdType(), requestParam.getIdCard());
        if (userDeletionNum >= 5) {
            throw new ClientException("证件号多次销号，已经拉入黑名单");
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
