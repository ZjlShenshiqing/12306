package org.openzjl.index12306.biz.orderservice.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderItemPassengerMapper;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.biz.orderservice.service.OrderPassengerRelationService;
import org.springframework.stereotype.Service;

/**
 * 乘车人订单关系接口层实现
 *
 * @author zhangjlk
 * @date 2026/1/15 17:33
 */
@Service
public class OrderPassengerRelationServiceImpl extends ServiceImpl<OrderItemPassengerMapper, OrderItemPassengerDO> implements OrderPassengerRelationService {
}
