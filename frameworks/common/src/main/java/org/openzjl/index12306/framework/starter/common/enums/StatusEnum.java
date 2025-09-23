package org.openzjl.index12306.framework.starter.common.enums;

/**
 * 状态枚举
 * @author zhangjlk
 * @date 2025/9/18 10:35
 */
public enum StatusEnum {

    /**
     * 成功
     */
    SUCCESS(0),

    /**
     * 失败
     */
    FAILURE(1);

    private final Integer statusCode;

    StatusEnum(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer code() {
        return this.statusCode;
    }

    public String strCode() {
        return String.valueOf(this.statusCode);
    }

    @Override
    public String toString() {
        return strCode();
    }
}
