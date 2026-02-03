/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.service.Impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderCanalErrorCodeEnum;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderDO;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderMapper;
import org.openzjl.index12306.biz.orderservice.dto.domain.OrderItemStatusReversalDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单明细服务接口实现类
 * 提供订单明细相关的业务逻辑处理，包括订单明细查询等功能
 * 继承 MyBatis-Plus 的 ServiceImpl，提供基础的 CRUD 操作
 *
 * @author zhangjlk
 * @date 2026/1/14 11:39
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItemDO> implements OrderItemService {

    /**
     * 订单明细数据访问对象
     */
    private final OrderItemMapper orderItemMapper;

    /**
     * 订单数据访问对象
     */
    private final OrderMapper orderMapper;

    /**
     * Redisson客户端，用于获取分布式锁
     * 用于防止并发修改订单状态
     */
    private final RedissonClient redissonClient;

    /**
     * 根据订单号和订单明细ID列表查询订单明细信息
     * 用于查询指定订单下的特定订单明细记录（乘客信息）
     *
     * @param requestParam 订单明细查询请求参数，包含订单号和订单明细ID列表
     * @return 订单明细响应对象列表，包含乘客详情信息
     */
    @Override
    public List<TicketOrderPassengerDetailRespDTO> queryTicketItemOrderById(TicketOrderItemQueryReqDTO requestParam) {
        // 构建订单明细查询条件：根据订单号精确匹配，且订单明细ID在指定列表中
        LambdaQueryWrapper<OrderItemDO> queryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn())
                .in(OrderItemDO::getId, requestParam.getOrderItemRecordIds());
        // 执行查询，获取符合条件的订单明细列表
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(queryWrapper);
        // 将订单明细实体列表转换为乘客详情响应DTO列表
        return BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class);
    }

    /**
     * 订单明细状态反转
     * 
     * 业务场景：
     * 用于订单和订单明细状态的灵活修改，通常用于以下场景：
     * 1. 部分退款：只更新部分订单明细的状态（如：订单中有3个乘客，只退1个乘客的票）
     * 2. 订单补偿：当订单明细状态异常时，需要手动修正特定订单明细的状态
     * 3. 状态回滚：当某些操作失败后，需要将特定订单明细的状态回滚
     * 4. 特殊业务处理：某些特殊业务场景需要精确控制每个订单明细的状态
     * 
     * 业务规则：
     * - 必须更新订单主表状态
     * - 可以选择性地更新指定的订单明细（通过 orderItemDOList 指定）
     * - 如果 orderItemDOList 为空，则不更新订单明细
     * - 更新订单明细时，使用订单号和真实姓名作为匹配条件（精确匹配特定乘客的订单明细）
     * - 状态值由调用方指定，不进行业务校验（需要调用方保证状态值的正确性）
     * 
     * 与 statusReversal 的区别：
     * - statusReversal：更新订单下所有订单明细的状态，需要订单状态为"待支付"
     * - orderItemStatusReversal：只更新指定的订单明细，不检查订单状态
     * - orderItemStatusReversal：使用订单号+真实姓名精确匹配订单明细（更精确）
     * 
     * 并发控制：
     * - 使用分布式锁（Redisson）防止同一订单被并发修改状态
     * - 锁的Key格式：order:status-reversal:order_sn_{订单号}
     * - 如果获取锁失败，只记录警告日志，不抛出异常（允许并发场景下的幂等性处理）
     * 
     * 注意事项：
     * - 本方法不进行业务状态校验，调用方需要确保状态值的正确性
     * - 建议仅在特殊场景下使用（如：部分退款、补偿、回滚、数据修复等）
     * - 获取锁失败时不会抛出异常，但也不会执行状态更新（保证幂等性）
     * - 状态更新失败时会抛出异常，需要调用方处理
     * - 订单明细更新时，如果找不到匹配的记录（订单号+真实姓名），会抛出异常
     *
     * @param requestParam 订单明细状态反转请求参数，包含：
     *                    - orderSn：订单号
     *                    - orderStatus：订单主表的目标状态
     *                    - orderItemStatus：订单明细表的目标状态
     *                    - orderItemDOList：需要更新的订单明细列表（可选，为空则不更新订单明细）
     * @throws ServiceException 订单不存在、更新失败时抛出
     */
    @Override
    public void orderItemStatusReversal(OrderItemStatusReversalDTO requestParam) {
        // 查询订单是否存在
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        
        // 校验订单是否存在
        // 订单不存在，抛出异常
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        }
        
        // 获取分布式锁，防止并发修改同一订单的状态
        // 锁的Key：order:status-reversal:order_sn_{订单号}
        RLock lock = redissonClient.getLock(StrBuilder.create("order:status-reversal:order_sn_").append(orderDO.getOrderSn()).toString());
        // 尝试获取锁，如果获取失败（返回false），说明订单正在被其他线程处理
        // 只记录警告日志，不抛出异常（保证幂等性，允许重复调用）
        if (!lock.tryLock()) {
            log.warn("订单重复修改状态，状态反转请求参数: {}", JSON.toJSONString(requestParam));
        }
        
        try {
            // 更新订单主表状态为目标状态（由请求参数指定）
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(requestParam.getOrderStatus());  // 使用请求参数中的目标状态
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
            int orderUpdateResult = orderMapper.update(updateOrderDO, updateWrapper);
            // 校验更新结果，如果更新行数<=0，说明更新失败
            if (orderUpdateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
            
            // 如果提供了订单明细列表，则更新指定的订单明细状态
            if (CollectionUtil.isNotEmpty(requestParam.getOrderItemDOList())) {
                List<OrderItemDO> orderItemDOList = requestParam.getOrderItemDOList();
                // 双重校验，确保列表不为空
                if (CollectionUtil.isNotEmpty(orderItemDOList)) {
                    // 遍历每个订单明细，逐个更新状态
                    orderItemDOList.forEach(orderItem -> {
                        // 构建订单明细更新对象，设置目标状态
                        OrderItemDO orderItemDO = new OrderItemDO();
                        orderItemDO.setStatus(requestParam.getOrderItemStatus());  // 使用请求参数中的目标状态
                        
                        // 构建更新条件：根据订单号和真实姓名精确匹配
                        // 这样可以精确更新特定乘客的订单明细（而不是更新所有订单明细）
                        LambdaUpdateWrapper<OrderItemDO> orderItemUpdateWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                                .eq(OrderItemDO::getOrderSn, orderItem.getOrderSn())      // 订单号匹配
                                .eq(OrderItemDO::getRealName, orderItem.getRealName());    // 真实姓名匹配（精确匹配特定乘客）
                        
                        // 执行更新操作
                        int orderItemUpdateResult = orderItemMapper.update(orderItemDO, orderItemUpdateWrapper);
                        // 校验更新结果，如果更新行数<=0，说明更新失败（可能是订单明细不存在或匹配条件不正确）
                        if (orderItemUpdateResult <= 0) {
                            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_ITEM_STATUS_REVERSAL_ERROR);
                        }
                    });
                }
            }
            // 如果 orderItemDOList 为空，则不更新订单明细（只更新订单主表状态）
        } finally {
            // 释放分布式锁，确保锁一定会被释放（即使发生异常）
            // 注意：如果之前获取锁失败，这里也会尝试释放（Redisson会处理这种情况）
            lock.unlock();
        }
    }
}
