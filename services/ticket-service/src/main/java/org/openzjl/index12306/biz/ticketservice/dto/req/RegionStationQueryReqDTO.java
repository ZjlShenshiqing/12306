/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.dto.req;

import lombok.Data;

/**
 * 地区 & 站点查询请求入参
 *
 * @author zhangjlk
 * @date 2025/11/22 16:36
 */
@Data
public class RegionStationQueryReqDTO {

    /**
     * 查询方式
     */
    private Integer queryType;

    /**
     * 名称
     */
    private String name;
}
