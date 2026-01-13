package org.openzjl.index12306.biz.userservice.controller;

import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserLoginRespDTO;
import org.openzjl.index12306.biz.userservice.service.UserLoginService;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.web.Results;
import org.springframework.web.bind.annotation.*;

/**
 * 用户登陆控制层
 *
 * @author zhangjlk
 * @date 2026/1/7 15:52
 */
@RestController
@RequiredArgsConstructor
public class UserLoginController {

    private final UserLoginService userLoginService;

    /**
     * 用户登录
     * @param requestParam 用户登录请求
     * @return 登录结果
     */
    @PostMapping("/api/user-service/v1/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userLoginService.login(requestParam));
    }

    /**
     * 通过 Token 检查用户是否登录
     * @param accessToken token
     * @return 用户是否登录
     */
    @GetMapping("/api/user-service/check-login")
    public Result<UserLoginRespDTO> checkLogin(@RequestParam("accessToken")  String accessToken) {
        UserLoginRespDTO result = userLoginService.checkLogin(accessToken);
        return Results.success(result);
    }

    /**
     * 用户退出登录
     *
     * @param accessToken 用户accessToken
     * @return 退出登录结果
     */
    @GetMapping("/api/user-service/logout")
    public Result<Void> logout(@RequestParam(required = true) String accessToken) {
        userLoginService.logout(accessToken);
        return Results.success();
    }
}
