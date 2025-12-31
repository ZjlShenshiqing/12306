package org.openzjl.index12306.biz.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * 交通工具座位类型
 *
 * @author zhangjlk
 * @date 2025/12/23 上午10:39
 */
@RequiredArgsConstructor
public enum VehicleSeatTypeEnum {

    /**
     * 商务座
     */
    BUSINESS_CLASS(0, "BUSINESS_CLASS", "商务座"),

    /**
     * 一等座
     */
    FIRST_CLASS(1, "FIRST_CLASS", "一等座"),

    /**
     * 二等座
     */
    SECOND_CLASS(2, "SECOND_CLASS", "二等座"),

    /**
     * 二等包座
     */
    SECOND_CLASS_CABIN_SEAT(3, "SECOND_CLASS_CABIN_SEAT", "二等包座"),

    /**
     * 一等卧
     */
    FIRST_SLEEPER(4, "FIRST_SLEEPER", "一等卧"),

    /**
     * 二等卧
     */
    SECOND_SLEEPER(5, "SECOND_SLEEPER", "二等卧"),

    /**
     * 软卧
     */
    SOFT_SLEEPER(6, "SOFT_SLEEPER", "软卧"),

    /**
     * 硬卧
     */
    HARD_SLEEPER(7, "HARD_SLEEPER", "硬卧"),

    /**
     * 硬座
     */
    HARD_SEAT(8, "HARD_SEAT", "硬座"),

    /**
     * 高级软卧
     */
    DELUXE_SOFT_SLEEPER(9, "DELUXE_SOFT_SLEEPER", "高级软卧"),

    /**
     * 动卧
     */
    DINING_CAR_SLEEPER(10, "DINING_CAR_SLEEPER", "动卧"),

    /**
     * 软座
     */
    SOFT_SEAT(11, "SOFT_SEAT", "软座"),

    /**
     * 特等座
     */
    FIRST_CLASS_SEAT(12, "FIRST_CLASS_SEAT", "特等座"),

    /**
     * 无座
     */
    NO_SEAT_SLEEPER(13, "NO_SEAT_SLEEPER", "无座"),

    /**
     * 其他
     */
    OTHER(14, "OTHER", "其他");

    @Getter
    private final Integer code;

    @Getter
    private final String name;

    @Getter
    private final String value;

    /**
     * 根据编码查找名称
     * 
     * <p>该方法通过遍历所有座位类型枚举值，匹配给定的编码，返回对应的名称。
     * 如果未找到匹配的编码，则返回 null。
     *
     * @param code 座位类型编码，不能为 null
     * @return 对应的座位类型名称（如 "BUSINESS_CLASS"），如果未找到则返回 null
     */
    public static String findNameByCode(Integer code) {
        // 将枚举所有值转换为流
        return Arrays.stream(VehicleSeatTypeEnum.values())
                // 过滤出编码匹配的枚举值
                .filter(each -> Objects.equals(each.getCode(), code))
                // 获取第一个匹配的枚举值
                .findFirst()
                // 如果找到，则提取其名称；否则返回 Optional.empty()
                .map(VehicleSeatTypeEnum::getName)
                // 如果未找到匹配项，返回 null
                .orElse(null);
    }
    
    /**
     * 根据编码查找值
     * 
     * <p>该方法通过遍历所有座位类型枚举值，匹配给定的编码，返回对应的值（中文描述）。
     * 如果未找到匹配的编码，则返回 null。
     *
     * @param code 座位类型编码，不能为 null
     * @return 对应的座位类型值（如 "商务座"），如果未找到则返回 null
     */
    public static String findValueByCode(Integer code) {
        // 将枚举所有值转换为流
        return Arrays.stream(VehicleSeatTypeEnum.values())
                // 过滤出编码匹配的枚举值
                .filter(each -> Objects.equals(each.getCode(), code))
                // 获取第一个匹配的枚举值
                .findFirst()
                // 如果找到，则提取值；否则返回 Optional.empty()
                .map(VehicleSeatTypeEnum::getValue)
                // 如果未找到匹配项，返回 null
                .orElse(null);
    }
}
