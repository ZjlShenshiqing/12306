/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.userservice.dto.req.UserDeletionReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import org.openzjl.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import org.openzjl.index12306.biz.userservice.dto.req.UserUpdateReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserQueryActualRespDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserRegisterRespDTO;
import org.openzjl.index12306.biz.userservice.service.UserLoginService;
import org.openzjl.index12306.biz.userservice.service.UserService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制层
 *
 * @author zhangjlk
 * @date 2026/1/13 16:38
 */
@RestController
@RequiredArgsConstructor
public class UserInfoController {

    private final UserService userService;
    private final UserLoginService userLoginService;

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/api/user-service/query")
    public Result<UserQueryRespDTO> queryUserByUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userService.queryUserByUsername(username));
    }

    /**
     * 根据用户名查询用户无脱敏信息
     *
     * @param username 用户名
     * @return 用户无脱敏信息
     */
    @GetMapping("/api/user-service/actual/query")
    public Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userService.queryActualUserByUserName(username));
    }

    /**
     * 检查用户名是否已经存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    @GetMapping("/api/user-service/has-username")
    public Result<Boolean> hasUserName(@RequestParam("username") @NotEmpty String username) {
        return Results.success(userLoginService.hasUserName(username));
    }

    /**
     * 用户注册
     *
     * @param requestParam 用户注册请求参数
     * @return 注册结果
     */
    @PostMapping("/api/user-service/register")
    public Result<UserRegisterRespDTO> register(@RequestBody @Valid UserRegisterReqDTO requestParam) {
        return Results.success(userLoginService.register(requestParam));
    }

    /**
     * 修改用户
     *
     * @param requestParam 用户修改请求参数
     * @return 修改结果
     */
    @PostMapping("/api/user-service/update")
    public Result<Void> update(@RequestBody @Valid UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }

    /**
     * 删除用户
     *
     * @param requestParam 用户删除请求参数
     * @return 删除结果
     */
    @PostMapping("/api/user-service/deletion")
    public Result<Void> deletion(@RequestBody @Valid UserDeletionReqDTO requestParam) {
        userLoginService.deletion(requestParam);
        return Results.success();
    }
}
