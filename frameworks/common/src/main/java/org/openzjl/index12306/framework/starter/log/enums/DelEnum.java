package org.openzjl.index12306.framework.starter.log.enums;

/**
 * 删除标记枚举
 * @author zhangjlk
 * @date 2025/9/18 10:34
 */
public enum DelEnum{

    /**
     * 正常状态
     */
    NORMAL(0),

    /**
     * 删除状态
     */
    DELETE(1);

    private final Integer statusCode;

    DelEnum(Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * 拿到状态对应的数字
     */
    public Integer code() {
        return this.statusCode;
    }

    /**
     * 把数字转成字符串
     * 有时候接口传的是字符串 "0" 而不是整数 0，就需要这个
     */
    public String strCode() {
        return String.valueOf(this.statusCode);
    }

    @Override
    public String toString() {
        return strCode();
    }
}
