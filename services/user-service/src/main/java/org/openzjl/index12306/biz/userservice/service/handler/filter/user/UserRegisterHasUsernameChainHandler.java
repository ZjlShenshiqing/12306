/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.service.handler.filter.user;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum;
import org.openzjl.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import org.openzjl.index12306.biz.userservice.service.UserLoginService;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

/**
 *
 * @author zhangjlk
 * @date 2026/1/13 18:10
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterHasUsernameChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO>{

    private final UserLoginService userLoginService;

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        if (!userLoginService.hasUserName(requestParam.getUsername())) {
            throw new ClientException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
