package org.openzjl.index12306.biz.userservice.service;

import jakarta.validation.constraints.NotEmpty;
import org.openzjl.index12306.biz.userservice.dto.req.UserQueryRespDTO;

/**
 *
 * @author zhangjlk
 * @date 2026/1/9 18:53
 */
public interface UserService {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户详细信息
     */
    UserQueryRespDTO queryUserByUsername(@NotEmpty String username);
}
