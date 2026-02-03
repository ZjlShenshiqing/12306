package org.openzjl.index12306.biz.ticketservice.mq.event;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Canal Binlog 监听触发时间
 *
 * @author zhangjlk
 * @date 2026/2/3 17:54
 */
@Data
public class CanalBinlogEvent {

    /**
     * 变更数据
     */
    private List<Map<String, Object>> data;

    /**
     * 数据库名称
     */
    private String database;

    /**
     * es 是指 Mysql Binlog原始的时间戳，也就是数据原始变更时间
     */
    private Long es;

    /**
     * 递增ID
     */
    private Long id;

    /**
     * 当前变更是否是 DDL 语句
     */
    private Boolean isDdl;

    /**
     * 表结构字段类型
     */
    private Map<String, Object> mysqlType;

    /**
     * UPDATE 模型下旧数据
     */
    private List<Map<String, Object>> old;

    /**
     * 主键名称
     */
    private List<String> pkNames;
}
