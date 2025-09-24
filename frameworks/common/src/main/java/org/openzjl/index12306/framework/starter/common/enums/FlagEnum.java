package org.openzjl.index12306.framework.starter.common.enums;

/**
 * 标识枚举
 *
 * 替代布尔值（boolean）的枚举版开关
 *
 * 用的是 数字 1 和 0 来代表 true 和 false，更适合数据库存储
 *
 * @author zhangjlk
 * @date 2025/9/18 10:34
 */
public enum FlagEnum {

    FALSE(0),

    TRUE(1);

    private final Integer flag;

    FlagEnum(Integer flag) {
        this.flag = flag;
    }

    public Integer code() {
        return this.flag;
    }

    public String strCode() {
        return String.valueOf(this.flag);
    }

    @Override
    public String toString() {
        return strCode();
    }
}
