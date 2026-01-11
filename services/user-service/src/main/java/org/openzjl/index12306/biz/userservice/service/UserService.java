package org.openzjl.index12306.biz.userservice.service;

import jakarta.validation.constraints.NotEmpty;
import org.openzjl.index12306.biz.userservice.dto.req.UserQueryRespDTO;
import org.openzjl.index12306.biz.userservice.dto.req.UserUpdateReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserQueryActualRespDTO;

/**
 *
 * @author zhangjlk
 * @date 2026/1/9 18:53
 */
public interface UserService {

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param userId 用户ID
     * @return       用户详细信息
     */
    UserQueryRespDTO queryUserByUserId(@NotEmpty String userId);

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户详细信息
     */
    UserQueryRespDTO queryUserByUsername(@NotEmpty String username);

    /**
     * 根据用户名查询用户无脱敏信息
     *
     * @param userName 用户名
     * @return 用户无脱敏信息
     */
    UserQueryActualRespDTO queryActualUserByUserName(@NotEmpty String userName);

    /**
     * 根据证件类型和证件号查询注销次数 (TODO：需要使用缓存提高查询速度)
     *
     * @param idType 证件类型
     * @param idCard 证件号
     * @return 注销次数
     */
    Integer queryUserDeletionNum(Integer idType, String idCard);

    /**
     * 根据用户 ID 修改用户信息
     *
     * @param requestParam 用户信息入参
     */
    void update(UserUpdateReqDTO requestParam);
}
