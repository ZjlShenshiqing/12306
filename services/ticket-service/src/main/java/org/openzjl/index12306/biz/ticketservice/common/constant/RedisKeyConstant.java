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

    /**
     * 列车站点缓存
     */
    public static final String STATION_ALL = "index12306-ticket-service:station_all:";

    /**
     * 列车车厢查询，Key Prefix + 列车ID
     */
    public static final String TRAIN_CARRIAGE = "index12306-ticket-service:train_carriage:";

    /**
     * 获取列车车厢数量集合分布式锁 Key
     */
    public static final String LOCK_QUERY_CARRIAGE_NUMBER_LIST = "index12306-ticket-service:lock:query_carriage_number_list_%s";

    /**
     * 站点余票查询，Key Prefix + 列车ID_起始站点_终点
     */
    public static final String TRAIN_STATION_REMAINING_TICKET = "index12306-ticket-service:train_station_remaining_ticket:";

    /**
     * 列车购买令牌桶 - 相当于余票
     */
    public static final String TICKET_AVAILABILITY_TOKEN_BUCKET = "index12306-ticket-service:ticket_availability_token_bucket:";
}
