/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderDO;

/**
 * 订单持久层
 *
 * @author zhangjlk
 * @date 2026/1/15 14:12
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {
}
