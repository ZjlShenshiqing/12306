package org.openzjl.index12306.biz.userservice.common.enums;

import lombok.AllArgsConstructor;
import org.openzjl.index12306.framework.starter.convention.errorcode.IErrorCode;

/**
 * 用户注册错误码枚举
 *
 * @author zhangjlk
 * @date 2026/1/9 10:41
 */
@AllArgsConstructor
public enum UserRegisterErrorCodeEnum implements IErrorCode {

    USER_REGISTER_FAIL("A006000", "用户注册失败"),

    HAS_USERNAME_NOTNULL("A006006", "用户名已存在"),

    PHONE_REGISTERED("A006007", "手机号已被占用"),

    MAIL_REGISTERED("A006008", "邮箱已被占用")
    ;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误提示信息
     */
    private final String message;


    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
