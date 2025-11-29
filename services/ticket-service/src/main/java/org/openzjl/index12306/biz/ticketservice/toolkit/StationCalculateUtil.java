package org.openzjl.index12306.biz.ticketservice.toolkit;

import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * 站点计算工具
 *
 * @author zhangjlk
 * @date 2025/11/29 09:56
 */
public class StationCalculateUtil {

    public static List<RouteDTO> throughStation(List<String> stations, String startStation, String endStation) {
        List<RouteDTO> routesToDeduct = new ArrayList<>();
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);
//        for (startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
//            return routesToDeduct;
//        }
        return List.of();
    }
}
