/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
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

    /**
     * SQL 语句
     */
    private String sql;

    /**
     * SQL 类型
     */
    private Map<String, Object> sqlType;

    /**
     * 表名
     */
    private String table;

    /**
     * ts 是指 Canal 收到这个 Binlog，构造为自己协议对象的时间
     * 应用消费的延迟 = now - ts
     */
    private Long ts;

    /**
     * INSERT（新增）、UPDATE（更新）、DELETE（删除）等等
     */
    private String type;
}
