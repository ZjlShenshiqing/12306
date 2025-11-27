package org.openzjl.index12306.biz.ticketservice.common.enums;

import cn.hutool.core.collection.ListUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

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

    //A_E(1, ListUtil.of("A", "B","C","D","E"));

    /**
     * 类型
     */
    @Getter
    private final Integer type;

    /**
     * 拼音列表
     */
    @Getter
    private final String spells;

    /**
     * 根据类型查找拼音集合
     */
//    public static List<String> findSpellsByType(Integer type) {
//        return Arrays.stream(RegionStationQueryTypeEnum)
//    }
}
