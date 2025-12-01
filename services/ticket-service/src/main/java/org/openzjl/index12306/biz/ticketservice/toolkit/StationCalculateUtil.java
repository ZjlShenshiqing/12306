package org.openzjl.index12306.biz.ticketservice.toolkit;

import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 站点计算工具
 *
 * @author zhangjlk
 * @date 2025/11/29 09:56
 */
public class StationCalculateUtil {

    /**
     * 计算出发站和终点站中间的站点（包含出发站和终点站）
     *
     * @param stations      所有站点数据
     * @param startStation  出发站
     * @param endStation    终点站
     * @return              出发站和终点站中间的站点
     */
    public static List<RouteDTO> throughStation(List<String> stations, String startStation, String endStation) {
        // 初始化结果列表，用于存储所有计算出的区间对象
        List<RouteDTO> routesToDeduct = new ArrayList<>();

        // 获取出发站和到达站在整条线路列表中的索引位置
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);

        // 校验逻辑
        // - startIndex < 0: 出发站不存在
        // - endIndex < 0: 到达站不存在
        // - startIndex >= endIndex: 出发站必须在到达站之前（不能反向，也不能是同一站）
        if (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return routesToDeduct; // 校验失败，返回空列表
        }

        // 生成所有子区间
        // 外层循环 i：遍历作为子区间的"出发站"，从本次行程的起点开始，直到终点前一站
        for (int i = startIndex; i < endIndex; i++) {
            // 内层循环 j：遍历作为子区间的"到达站"，从 i 的下一站开始，直到本次行程的终点
            for (int j = i + 1; j <= endIndex; j++) {

                // 获取具体的站名
                String currentStation = stations.get(i); // 子区间的起点
                String nextStation = stations.get(j);    // 子区间的终点

                RouteDTO routeDTO = new RouteDTO(currentStation, nextStation);

                // 加入结果集
                routesToDeduct.add(routeDTO);
            }
        }

        // 返回所有受影响的区间列表
        return routesToDeduct;
    }

    /**
     * 计算因购买 "出发站" 到 "终点站" 的车票，导致哪些区间的库存需要被扣减。
     * (即：找出所有与 [startStation, endStation] 这一段路程有物理重叠的区间)
     *
     * @param stations      整条线路的所有站点列表，例如 [A, B, C, D, E]
     * @param startStation  本次购买的出发站，例如 B
     * @param endStation    本次购买的终点站，例如 D
     * @return              所有受影响需要扣减库存的区间列表
     */
    public static List<RouteDTO> takeoutStation(List<String> stations, String startStation, String endStation) {
        List<RouteDTO> takeoutStationList = new ArrayList<>();

        int startIndex = stations.indexOf(startStation); // 本次购买的起始站的索引
        int endIndex = stations.indexOf(endStation);     // 本次购买的终点站的索引

        // 基础校验：站点必须存在，且出发站必须在终点站之前
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return takeoutStationList;
        }

        // ---------------------------------------------------------
        // 核心逻辑第一部分：处理"起点在 startStation 之前"的受影响区间
        // 场景：比如买了 B->D。那么 A->C, A->D, A->E 都会受到影响，因为它们都经过 B-C 或 C-D 段。
        // 但是 A->B 不受影响，因为路段没重叠
        // ---------------------------------------------------------
        if (startIndex != 0) {
            // 遍历出发站之前的所有站点
            for (int i = 0; i < startIndex; i++) {
                // j: 从 1 开始，为了跳过出发站本身
                for (int j = i + 1; j < stations.size() - startIndex; j++) {
                    // stations.get(startIndex + j): 只有当这条路线的终点 越过了 startIndex，就会产生重叠路段
                    takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(startIndex + j)));
                }
            }
        }

        // ---------------------------------------------------------
        // 核心逻辑第二部分：处理"起点在 startStation 之后(含)"的受影响区间
        // 场景：比如买了 B->D。那么 B->C, B->E, C->D, C->E 都会受影响。
        // ---------------------------------------------------------
        // i: 遍历从 startStation 到 endStation 之间的站点
        for (int i = startIndex; i <= endIndex; i++) {
            // j: 遍历 i 之后的所有站点
            // 条件 i < endIndex 是为了防止 i 已经到了终点站，虽然外层循环允许 i=endIndex，但内层逻辑实际上需要 i 作为起点。
            // 逻辑：只要起点 i 还没到本次行程的终点 D，那么从 i 出发往后的票都会受影响。
            for (int j = i + 1; j < stations.size() && i < endIndex; j++) {
                takeoutStationList.add(new RouteDTO(stations.get(i), stations.get(j)));
            }
        }

        return takeoutStationList;
    }

    public static void main(String[] args) {
        List<String> stations = Arrays.asList("北京南", "济南西", "南京南", "杭州东", "宁波");
        String startStation = "北京南";
        String endStation = "南京南";
        StationCalculateUtil.takeoutStation(stations, startStation, endStation).
                forEach(System.out::println);
    }
}
