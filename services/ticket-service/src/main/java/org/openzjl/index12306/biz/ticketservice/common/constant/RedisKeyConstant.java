package org.openzjl.index12306.biz.ticketservice.common.constant;

/**
 * Redis Key 定义常量类
 *
 * @author zhangjlk
 * @date 2025/11/26 15:53
 */
public class RedisKeyConstant {

    /**
     * 地区以及车站查询，Key Prefix + ( 车站名称 or 查询方式)
     */
    public static final String REGION_STATION = "index12306-ticket-service:region_station:";

    /**
     * 获取地区以及站点集合分布式锁 Key
     */
    public static final String LOCK_QUERY_REGION_STATION_LIST = "index12306-ticket-service:lock_query_region_station_list_%s:";
}
