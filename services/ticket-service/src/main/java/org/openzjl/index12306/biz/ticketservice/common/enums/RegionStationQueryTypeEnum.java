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

/**
 * 地区 & 站点类型枚举
 *
 * @author zhangjlk
 * @date 2025/11/27 10:04
 */
@RequiredArgsConstructor
public enum RegionStationQueryTypeEnum {

    /**
     * 热门查询
     */
    HOT(0, null),

    /**
     * A-E 段
     * - 代表首字母在 A、B、C、D、E 这几个字母内的车站
     */
    A_E(1, ListUtil.of("A", "B","C","D","E")),

    /**
     * F-J 段
     */
    F_J(2, ListUtil.of("F", "G","H","J")),

    /**
     * K-O 段
     */
    K_O(3, ListUtil.of("K", "L","M","N","O")),

    /**
     * P-T 段
     */
    P_T(4, ListUtil.of("P", "Q","R","S","T")),

    /**
     * U-Z 段
     * - 注意不包含 W，这里是按你的业务划分来的
     */
    U_Z(5, ListUtil.of("U", "V","X","Y","Z"));

    /**
     * 类型编码
     *
     * 用于前端/接口传参，比如：
     *  - 0：热门查询
     *  - 1：A-E
     *  - 2：F-J
     *  - ...
     */
    @Getter
    private final Integer type;

    /**
     * 拼音首字母列表
     *
     * 说明：
     *  - 对于 HOT 类型，这个字段为 null
     *  - 对于其他类型，是一个字母列表，例如 ["A","B","C","D","E"]
     *  - 下游可以拿这个列表去筛选车站的拼音首字母
     */
    @Getter
    private final List<String> spells;

    /**
     * 根据类型查找拼音首字母集合
     *
     * @param type 传入的类型编码（与上面的 enum 中的 type 一致）
     * @return 对应类型的字母列表：
     *         - 如果能匹配到：返回该枚举上的 spells
     *         - 如果找不到：返回 null
     *
     * 使用示例：
     *   List<String> spells = RegionStationQueryTypeEnum.findSpellsByType(1);
     *   // spells = ["A","B","C","D","E"]
     */
    public static List<String> findSpellsByType(Integer type) {
        return Arrays.stream(RegionStationQueryTypeEnum.values()) // 遍历所有枚举值
                // 过滤出 type 相等的枚举；用 Objects.equals 防止 NPE
                .filter(each -> Objects.equals(each.getType(), type))
                // 找到第一个匹配的
                .findFirst()
                // 把枚举对象映射成它的 spells 字段
                .map(RegionStationQueryTypeEnum::getSpells)
                // 如果没找到任何枚举，返回 null
                .orElse(null);
    }
}
