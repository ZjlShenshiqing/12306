package org.openzjl.index12306.biz.userservice.service;

import org.openzjl.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserLoginRespDTO;

/**
 * 用户登陆接口
 *
 * @author zhangjlk
 * @date 2026/1/7 15:55
 */
public interface UserLoginService {

    /**
     * 用户登录接口
     *
     * @param requestParam 用户登录入参
     * @return 用户登录返回结果
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);
}
