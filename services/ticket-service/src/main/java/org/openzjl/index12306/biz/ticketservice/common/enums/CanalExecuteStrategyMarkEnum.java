package org.openzjl.index12306.biz.ticketservice.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Canal 执行策略标记枚举
 * <p>
 * 定义 Canal 数据同步时的表名映射策略，用于处理分表场景。
 * 包含实际表名和对应的模式匹配表名（正则表达式），
 * 支持判断表名是否匹配特定模式以及根据实际表名获取对应的模式匹配表名。
 * </p>
 * 
 * <p><strong>设计用途：</strong></p>
 * <ul>
 *   <li>用于 Canal 数据同步时的表名识别和处理</li>
 *   <li>支持分表场景，通过正则表达式匹配分表表名</li>
 *   <li>提供表名模式匹配和查询功能</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2026/2/3 18:29
 */
@RequiredArgsConstructor
public enum CanalExecuteStrategyMarkEnum {

    /**
     * 座位表
     * <p>
     * 对应数据库中的座位表，实际表名为 "t_seat"，
     * 无分表场景，因此模式匹配表名为 null。
     * </p>
     */
    T_SEAT("t_seat", null),

    /**
     * 订单表
     * <p>
     * 对应数据库中的订单表，实际表名为 "t_order"，
     * 存在分表场景，模式匹配表名为正则表达式 "^t_order_([0-9]+|1[0-6])"，
     * 匹配格式为 "t_order_" 后跟数字（1-16）的分表表名。
     * </p>
     * <p>
     * 正则表达式说明：
     * <ul>
     *   <li>^t_order_：以 "t_order_" 开头</li>
     *   <li>([0-9]+|1[0-6])：匹配数字或 10-16
     *     <ul>
     *       <li>[0-9]+：匹配一个或多个数字（0-9）</li>
     *       <li>|：或</li>
     *       <li>1[0-6]：匹配 10-16</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     */
    T_ORDER("t_order", "^t_order_([0-9]+|1[0-6])");

    /**
     * 实际表名
     * <p>
     * 数据库中真实存在的表名，如 "t_seat"、"t_order"。
     * </p>
     */
    @Getter
    private final String actualTable;

    /**
     * 模式匹配表名
     * <p>
     * 用于匹配分表表名的正则表达式，如 "^t_order_([0-9]+|1[0-6])"。
     * 如果不存在分表场景，则为 null。
     * </p>
     */
    @Getter
    private final String patternMatchTable;

    /**
     * 判断表名是否匹配任何模式
     * <p>
     * 遍历所有枚举常量，检查表名是否匹配任何模式匹配表名（正则表达式）。
     * </p>
     *
     * @param tableName 表名
     *                 <ul>
     *                   <li>待检查的表名，如 "t_order_1"、"t_seat"</li>
     *                 </ul>
     * @return 是否匹配任何模式
     *         <ul>
     *           <li>true：表名匹配某个模式</li>
     *           <li>false：表名不匹配任何模式</li>
     *         </ul>
     * 
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>遍历所有 CanalExecuteStrategyMarkEnum 枚举常量</li>
     *   <li>对于每个常量，检查其模式匹配表名是否不为空</li>
     *   <li>如果不为空，编译正则表达式并匹配表名</li>
     *   <li>如果找到任何匹配的模式，返回 true</li>
     *   <li>如果遍历完成后没有找到匹配的模式，返回 false</li>
     * </ol>
     */
    public static boolean isPatternMatch(String tableName) {
        return Arrays.stream(CanalExecuteStrategyMarkEnum.values())
                .anyMatch(each -> StrUtil.isNotBlank(each.getPatternMatchTable()) && Pattern.compile(each.getPatternMatchTable()).matcher(tableName).matches());
    }

    /**
     * 根据实际表名获取对应的模式匹配表名
     * <p>
     * 根据给定的实际表名，查找对应的枚举常量，并返回其模式匹配表名。
     * </p>
     *
     * @param tableName 实际表名
     *                 <ul>
     *                   <li>数据库中真实存在的表名，如 "t_order"、"t_seat"</li>
     *                 </ul>
     * @return 模式匹配表名
     *         <ul>
     *           <li>如果找到对应的枚举常量，返回其模式匹配表名（可能为 null）</li>
     *           <li>如果未找到对应的枚举常量，返回 null</li>
     *         </ul>
     * 
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>遍历所有 CanalExecuteStrategyMarkEnum 枚举常量</li>
     *   <li>对于每个常量，检查其实际表名是否与给定表名相等</li>
     *   <li>如果找到匹配的常量，返回其模式匹配表名</li>
     *   <li>如果遍历完成后没有找到匹配的常量，返回 null</li>
     * </ol>
     */
    public static String getPatternMatch(String tableName) {
        return Arrays.stream(CanalExecuteStrategyMarkEnum.values())
                .filter(each -> Objects.equals(tableName, each.getActualTable()))
                .findFirst()
                .map(CanalExecuteStrategyMarkEnum::getPatternMatchTable)
                .orElse(null);
    }
}
