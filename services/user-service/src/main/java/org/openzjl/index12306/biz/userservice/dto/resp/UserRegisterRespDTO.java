package org.openzjl.index12306.biz.userservice.dto.resp;

import lombok.Data;

/**
 * 用户注册返回参数
 *
 * @author zhangjlk
 * @date 2026/1/9 10:14
 */
@Data
public class UserRegisterRespDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 电话号码
     */
    private String phone;
}
