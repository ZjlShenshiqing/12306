/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.dto.resp;

import lombok.Data;

/**
 * 地区 & 站点分页查询响应参数
 *
 * @author zhangjlk
 * @date 2025/11/25 15:02
 */
@Data
public class RegionStationQueryRespDTO {

    /**
     * 名称
     */
    private String name;

    /**
     * 编码
     */
    private String code;

    /**
     * 拼音
     */
    private String spell;
}
