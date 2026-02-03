/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.common.enums;

import cn.hutool.core.collection.ListUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.openzjl.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum.*;

/**
 * 交通工具类型
 *
 * @author zhangjlk
 * @date 2025/12/23 上午9:55
 */
@RequiredArgsConstructor
public enum VehicleTypeEnum {

    /**
     * 高速铁路
     */
    HIGH_SPEED_RAILWAY(0, "HIGH_SPEED_RAILWAY", "高速铁路", ListUtil.of(BUSINESS_CLASS.getCode(), FIRST_CLASS.getCode(), SECOND_CLASS.getCode())),

    /**
     * 动车
     */
    BULLET(1, "BULLET", "动车", ListUtil.of(SECOND_CLASS_CABIN_SEAT.getCode(), FIRST_SLEEPER.getCode(), SECOND_SLEEPER.getCode(), NO_SEAT_SLEEPER.getCode())),

    /**
     * 普通车
     */
    REGULAR_TRAIN(2, "REGULAR_TRAIN", "普通车", ListUtil.of(SOFT_SLEEPER.getCode(), HARD_SLEEPER.getCode(), HARD_SEAT.getCode(), NO_SEAT_SLEEPER.getCode())),

    /**
     * 汽车
     */
    CAR(3, "CAR", "汽车", null),

    /**
     * 飞机
     */
    AIRPLANE(4, "AIRPLANE", "飞机", null);

    @Getter
    private final Integer code;

    @Getter
    private final String name;

    @Getter
    private final String value;

    @Getter
    private final List<Integer> seatTypes;

    /**
     * 根据编码查找名称
     * 
     * <p>该方法通过遍历所有交通工具类型枚举值，匹配给定的编码，返回对应的名称。
     * 如果未找到匹配的编码，则返回 null。
     *
     * @param code 交通工具类型编码，不能为 null
     * @return 对应的交通工具类型名称（如 "HIGH_SPEED_RAILWAY"），如果未找到则返回 null
     */
    public static String findNameByCode(Integer code) {
        // 将枚举所有值转换为流
        return Arrays.stream(VehicleTypeEnum.values())
                // 过滤出编码匹配的枚举值
                .filter(each -> Objects.equals(each.getCode(), code))
                // 获取第一个匹配的枚举值
                .findFirst()
                // 如果找到，则提取其名称；否则返回 Optional.empty()
                .map(VehicleTypeEnum::getName)
                // 如果未找到匹配项，返回 null
                .orElse(null);
    }

    /**
     * 根据编码查找座位类型列表
     * 
     * <p>该方法通过遍历所有交通工具类型枚举值，匹配给定的编码，返回该交通工具类型支持的座位类型编码列表。
     * 如果未找到匹配的编码，则返回 null。
     * 注意：某些交通工具类型（如汽车、飞机）的座位类型列表可能为 null。
     *
     * @param code 交通工具类型编码，不能为 null
     * @return 对应的座位类型编码列表，如果未找到则返回 null
     */
    public static List<Integer> findSeatTypeByCode(Integer code) {
        // 将枚举所有值转换为流
        return Arrays.stream(VehicleTypeEnum.values())
                // 过滤出编码匹配的枚举值
                .filter(each -> Objects.equals(each.getCode(), code))
                // 获取第一个匹配的枚举值
                .findFirst()
                // 如果找到，则提取其座位类型列表；否则返回 Optional.empty()
                .map(VehicleTypeEnum::getSeatTypes)
                // 如果未找到匹配项，返回 null
                .orElse(null);
    }
}
