/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.service.Impl;

import cn.crane4j.annotation.AutoOperate;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderCanalErrorCodeEnum;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderItemStatusEnum;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderDO;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderMapper;
import org.openzjl.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.*;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.mq.event.DelayCloseOrderEvent;
import org.openzjl.index12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import org.openzjl.index12306.biz.orderservice.mq.produce.DelayCloseOrderSendProduce;
import org.openzjl.index12306.biz.orderservice.remote.UserRemoteService;
import org.openzjl.index12306.biz.orderservice.remote.dto.UserQueryActualRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.biz.orderservice.service.OrderPassengerRelationService;
import org.openzjl.index12306.biz.orderservice.service.OrderService;
import org.openzjl.index12306.biz.orderservice.service.orderid.OrderIdGeneratorManager;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.convention.page.PageResponse;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.database.toolkit.PageUtil;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 订单服务接口实现类
 * 提供订单相关的业务逻辑处理，包括订单查询、订单详情获取等功能
 *
 * @author zhangjlk
 * @date 2026/1/14 11:38
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /**
     * 订单数据访问对象
     */
    private final OrderMapper orderMapper;

    /**
     * 订单明细数据访问对象
     */
    private final OrderItemMapper orderItemMapper;

    /**
     * 订单乘客关系服务
     */
    private final OrderPassengerRelationService orderPassengerRelationService;

    /**
     * 订单明细服务
     */
    private final OrderItemService orderItemService;

    /**
     * 延迟关闭订单消息生产者
     */
    private final DelayCloseOrderSendProduce delayCloseOrderSendProduce;

    /**
     * Redisson客户端，用于获取分布式锁
     * 用于防止并发关闭同一订单
     */
    private final RedissonClient redissonClient;

    /**
     * 用户远程服务
     */
    private UserRemoteService userRemoteService;

    /**
     * 根据订单号查询车票订单详情
     * 包括订单基本信息以及订单关联的乘客明细信息
     *
     * @param orderSn 订单号
     * @return 订单详情响应对象，包含订单基本信息和乘客明细列表
     */
    @Override
    public TicketOrderDetailRespDTO queryTicketByOrderSn(String orderSn) {
        // 构建订单查询条件：根据订单号查询
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        // 查询订单主表信息
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        // 将订单实体转换为响应DTO
        TicketOrderDetailRespDTO result = BeanUtil.convert(orderDO, TicketOrderDetailRespDTO.class);
        
        // 构建订单明细查询条件：根据订单号查询所有订单明细
        LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, orderSn);
        // 查询订单明细列表（包含所有乘客信息）
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
        // 将订单明细实体列表转换为乘客详情DTO列表，并设置到结果对象中
        result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
        return result;
    }

    /**
     * 分页查询车票订单列表
     * 根据用户ID和订单状态类型进行筛选，按订单创建时间倒序排列
     * 查询结果包含订单基本信息和每个订单关联的乘客明细信息
     *
     * @param requestParam 订单分页查询请求参数，包含用户ID、状态类型、分页信息等
     * @return 分页响应对象，包含订单详情列表（每个订单包含乘客明细信息）
     */
    @Override
    @AutoOperate(type = TicketOrderDetailRespDTO.class, on = "data.records")
    public PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        // 构建订单查询条件：根据用户ID精确匹配，订单状态在指定状态列表中，按订单创建时间倒序排列
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, requestParam.getUserId())
                .in(OrderDO::getStatus, buildOrderStatusList(requestParam))
                .orderByDesc(OrderDO::getOrderTime);
        // 执行分页查询，获取订单分页数据
        IPage<OrderDO> orderPage = orderMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
        // 将分页结果转换为响应DTO，并为每个订单填充乘客明细信息
        return PageUtil.convert(orderPage, each -> {
            // 将订单实体转换为订单详情响应DTO
            TicketOrderDetailRespDTO result = BeanUtil.convert(each, TicketOrderDetailRespDTO.class);
            // 构建订单明细查询条件：根据订单号查询该订单的所有明细
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, each.getOrderSn());
            // 查询订单明细列表（包含该订单的所有乘客信息）
            List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
            // 将订单明细实体列表转换为乘客详情DTO列表，并设置到订单详情响应对象中
            result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
            return result;
        });
    }

    /**
     * 分页查询当前登录用户作为乘客的订单列表
     * 通过当前登录用户的身份证号，查询该用户作为乘客的所有订单记录
     * 返回包含订单详细信息的列表，按创建时间倒序排列
     *
     * @param requestParam 订单分页查询请求参数，包含分页信息
     * @return 分页响应对象，包含当前用户作为乘客的订单详情列表
     */
    @Override
    public PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        // 通过远程服务获取当前登录用户的真实信息（包含身份证号）
        Result<UserQueryActualRespDTO> userActualResp = userRemoteService.queryActualUserByUsername(UserContext.getUserName());
        // 构建订单乘客关系查询条件：根据身份证号精确匹配，按创建时间倒序排列
        LambdaQueryWrapper<OrderItemPassengerDO> queryWrapper = Wrappers.lambdaQuery(OrderItemPassengerDO.class)
                .eq(OrderItemPassengerDO::getIdCard, userActualResp.getData().getIdCard())
                .orderByDesc(OrderItemPassengerDO::getCreateTime);
        // 执行分页查询，获取当前用户作为乘客的订单关系记录
        IPage<OrderItemPassengerDO> orderItemPassengerPage = orderPassengerRelationService.page(PageUtil.convert(requestParam), queryWrapper);
        // 将分页结果转换为响应DTO，并为每条记录填充完整的订单信息
        return PageUtil.convert(orderItemPassengerPage, each -> {
            // 根据订单号查询订单主表信息
            LambdaQueryWrapper<OrderDO> orderQueryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                    .eq(OrderDO::getOrderSn, each.getOrderSn());
            OrderDO orderDO = orderMapper.selectOne(orderQueryWrapper);
            // 构建订单明细查询条件：根据订单号和身份证号精确匹配，查询该乘客的订单明细
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderDO.getOrderSn())
                    .eq(OrderItemDO::getIdCard, each.getIdCard());
            // 查询订单明细信息
            OrderItemDO orderItemDO = orderItemMapper.selectOne(orderItemQueryWrapper);
            // 将订单明细实体转换为响应DTO
            TicketOrderDetailSelfRespDTO actualResult = BeanUtil.convert(orderItemDO, TicketOrderDetailSelfRespDTO.class);
            // 忽略空值和空字符串，补充转换订单明细数据到响应对象
            BeanUtil.convertIgnoreNullAndBlank(orderItemDO, actualResult);
            return actualResult;
        });
    }

    /**
     * 创建车票订单
     * 
     * 业务场景：
     * 当用户在购票服务中选择车票并确认购买后，购票服务会调用本方法创建订单
     * 订单创建后会锁定车票库存，等待用户支付
     * 
     * 创建流程：
     * 1. 生成全局唯一的订单号
     * 2. 创建订单主表记录（包含订单基本信息）
     * 3. 批量创建订单明细记录（每个乘客一张车票）
     * 4. 批量创建订单乘客关系记录（用于查询乘客的订单）
     * 5. 发送延迟关闭订单消息（如果用户未在规定时间内支付，自动关闭订单）
     * 
     * 订单状态：
     * - 创建时状态为"待支付"（PENDING_PAYMENT）
     * - 订单明细状态为0（待支付）
     * 
     * 注意事项：
     * - 订单创建是事务性的，如果任何步骤失败，整个订单创建会回滚
     * - 延迟消息发送失败会抛出异常，但不影响订单创建（消息发送在事务外）
     *
     * @param requestParam 订单创建请求参数，包含用户信息、车次信息、订单明细等
     * @return 订单号，用于后续订单查询和支付
     */
    @Override
    public String createTicketOrder(TicketOrderCreateReqDTO requestParam) {
        // 生成全局唯一的订单号
        // 订单号格式：分布式ID生成器生成的ID + 用户ID的后6位
        String orderSn = OrderIdGeneratorManager.generateId(requestParam.getUserId());
        
        // 构建订单主表实体对象
        OrderDO orderDO = OrderDO.builder()
                .orderSn(orderSn)                                                    // 订单号
                .orderTime(requestParam.getOrderTime())                              // 订单创建时间
                .departure(requestParam.getDeparture())                              // 出发站编码
                .departureTime(requestParam.getDepartureTime())                      // 出发时间
                .ridingDate(requestParam.getRidingDate())                             // 乘车日期
                .arrivalTime(requestParam.getArrivalTime())                           // 到达时间
                .trainNumber(requestParam.getTrainNumber())                           // 车次号（如：G123）
                .arrival(requestParam.getArrival())                                   // 到达站编码
                .trainId(requestParam.getTrainId())                                   // 车次ID
                .source(requestParam.getSource())                                     // 订单来源（如：互联网）
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())                  // 订单状态：待支付
                .username(requestParam.getUsername())                                 // 用户名
                .userId(String.valueOf(requestParam.getUserId()))                      // 用户ID（转换为字符串）
                .build();
        // 保存订单主表记录
        orderMapper.insert(orderDO);
        
        // 构建订单明细和订单乘客关系数据
        List<TicketOrderItemCreateReqDTO> ticketOrderItems = requestParam.getTicketOrderItems();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<OrderItemPassengerDO> orderItemPassengerDOList = new ArrayList<>();
        
        // 遍历每个订单明细（每个乘客一张车票）
        ticketOrderItems.forEach(item -> {
            // 构建订单明细实体：包含车票的详细信息（座位、车厢、乘客信息、金额等）
            OrderItemDO orderItemDO = OrderItemDO.builder()
                    .trainId(requestParam.getTrainId())                               // 车次ID
                    .seatNumber(item.getSeatNumber())                                  // 座位号
                    .carriageNumber(item.getCarriageNumber())                          // 车厢号
                    .realName(item.getRealName())                                      // 乘客真实姓名
                    .orderSn(orderSn)                                                  // 订单号（关联订单主表）
                    .phone(item.getPhone())                                            // 乘客手机号
                    .seatType(item.getSeatType())                                      // 座位类型（如：一等座、二等座）
                    .username(requestParam.getUsername())                             // 用户名
                    .amount(item.getAmount())                                           // 订单金额
                    .carriageNumber(item.getCarriageNumber())                          // 车厢号（重复字段，可能是代码冗余）
                    .idCard(item.getIdCard())                                          // 乘客身份证号
                    .ticketType(item.getTicketType())                                  // 车票类型（如：成人票、儿童票）
                    .userId(String.valueOf(requestParam.getUserId()))                  // 用户ID
                    .status(0)                                                         // 订单明细状态：0表示待支付
                    .build();
            orderItemDOList.add(orderItemDO);
            
            // 构建订单乘客关系实体：用于建立订单与乘客的关联关系
            // 便于后续通过乘客身份证号查询该乘客的所有订单
            OrderItemPassengerDO orderItemPassengerDO = OrderItemPassengerDO.builder()
                    .idType(item.getIdType())                                          // 证件类型
                    .idCard(item.getIdCard())                                          // 乘客身份证号
                    .orderSn(orderSn)                                                  // 订单号（关联订单主表）
                    .build();
            orderItemPassengerDOList.add(orderItemPassengerDO);
        });
        
        // 批量保存订单明细记录
        orderItemService.saveBatch(orderItemDOList);
        // 批量保存订单乘客关系记录
        orderPassengerRelationService.saveBatch(orderItemPassengerDOList);
        
        // 发送延迟关闭订单消息
        // 如果用户在10分钟内未支付，系统会自动关闭订单并释放车票库存
        try {
            // 构建延迟关闭订单事件对象
            DelayCloseOrderEvent delayCloseOrderEvent = DelayCloseOrderEvent.builder()
                    .trainId(String.valueOf(requestParam.getTrainId()))                // 车次ID
                    .departure(requestParam.getDeparture())                            // 出发站编码
                    .arrival(requestParam.getArrival())                                 // 到达站编码
                    .orderSn(orderSn)                                                  // 订单号
                    .trainPurchaseTicketResults(requestParam.getTicketOrderItems())     // 订单明细列表（用于释放车票库存）
                    .build();
            // 发送延迟消息到RocketMQ（延迟级别14，约10分钟后投递）
            SendResult sendResult = delayCloseOrderSendProduce.sendMessage(delayCloseOrderEvent);
            // 校验消息发送结果，如果发送失败则抛出异常
            if (!Objects.equals(sendResult.getSendStatus(), SendStatus.SEND_OK)) {
                throw new ServiceException("投递延迟关闭订单消息队列失败");
            }
        } catch (Throwable ex) {
            // 记录错误日志，包含请求参数和异常信息
            log.error("延迟关闭订单消息队列发送错误，请求参数：{}", JSON.toJSONString(requestParam), ex);
            // 重新抛出异常，让调用方处理
            throw ex;
        }
        
        // 返回订单号，供调用方使用（如：用于支付、查询等）
        return orderSn;
    }

    /**
     * 关闭订单
     * 
     * 业务场景：
     * 系统自动关闭订单（超时未支付，由延迟消息触发）
     * 
     * 业务规则：
     * - 只能关闭"待支付"状态的订单
     * - 已支付、已完成、已取消的订单不能再次关闭
     * - 关闭订单会同时更新订单主表和订单明细表的状态
     * 
     * 并发控制：
     * - 使用分布式锁（Redisson）防止同一订单被并发关闭
     * - 锁的Key格式：order:canal:order_sn_{订单号}
     * - 如果获取锁失败，说明订单正在被其他线程处理，直接返回错误
     * 
     * 事务保证：
     * - 使用 @Transactional 注解，保证订单主表和明细表状态更新的原子性
     * - 如果任何更新失败，整个操作会回滚
     * 
     * 注意事项：
     * - 关闭订单后，车票库存的释放由消息消费者处理（不在本方法中）
     * - 订单关闭后，用户无法再支付该订单
     *
     * @param requestParam 取消订单请求参数，包含订单号
     * @return true 表示订单关闭成功
     * @throws ServiceException 订单不存在、订单状态不正确、更新失败时抛出
     * @throws ClientException 获取分布式锁失败时抛出（订单正在被处理）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean closeTicketOrder(CancelTicketOrderReqDTO requestParam) {
        // 获取订单号
        String orderSn = requestParam.getOrderSn();
        
        // 查询订单是否存在
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        
        // 校验订单状态
        // 订单不存在，抛出异常
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } 
        // 订单状态不是"待支付"，不能关闭（只能关闭待支付的订单）
        else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        
        // 获取分布式锁，防止并发关闭同一订单
        // 锁的Key：order:canal:order_sn_{订单号}
        RLock lock = redissonClient.getLock(StrBuilder.create("order:canal:order_sn_").append(orderSn).toString());
        // 尝试获取锁，如果获取失败（返回false），说明订单正在被其他线程处理
        if (!lock.tryLock()) {
            throw new ClientException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        
        try {
            // 更新订单主表状态为"已取消"
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(OrderStatusEnum.CLOSED.getStatus());  // 状态：已取消（30）
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, orderSn);
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            // 校验更新结果，如果更新行数<=0，说明更新失败（可能订单已被其他线程修改）
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
            
            // 批量更新订单明细表状态为"已取消"
            // 更新该订单下所有订单明细的状态
            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(OrderItemStatusEnum.CLOSED.getStatus());  // 订单明细状态：已取消
            LambdaUpdateWrapper<OrderItemDO> updateItemWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderSn);
            int updateItemResult = orderItemMapper.update(updateOrderItemDO, updateItemWrapper);
            // 校验更新结果，如果更新行数<=0，说明更新失败
            if (updateItemResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
        } finally {
            // 释放分布式锁，确保锁一定会被释放（即使发生异常）
            lock.unlock();
        }
        
        // 返回成功标识
        return true;
    }

    /**
     * 用户主动取消订单
     * 
     * 业务场景：
     * 用户在订单创建后、支付前，主动取消订单
     * 通常发生在用户改变主意、选错车次、或发现订单信息有误等情况
     * 
     * 业务规则：
     * - 只能取消"待支付"状态的订单
     * - 已支付、已完成、已取消的订单不能再次取消
     * - 取消订单会同时更新订单主表和订单明细表的状态为"已取消"
     * 
     * 并发控制：
     * - 使用分布式锁（Redisson）防止同一订单被并发取消
     * - 锁的Key格式：order:canal:order_sn_{订单号}
     * - 如果获取锁失败，说明订单正在被其他线程处理（可能是用户重复点击或系统正在关闭），返回"订单重复取消"错误
     * 
     * 与 closeTicketOrder 的区别：
     * - closeTicketOrder：系统自动关闭订单（超时未支付），使用 @Transactional 保证事务
     * - cancelTicketOrder：用户主动取消订单，不使用事务注解（由调用方控制事务）
     * - 获取锁失败时的错误码不同：cancelTicketOrder 返回"订单重复取消"，closeTicketOrder 返回"订单状态错误"
     * 
     * 注意事项：
     * - 取消订单后，车票库存的释放由消息消费者处理（不在本方法中）
     * - 订单取消后，用户无法再支付该订单
     * - 建议在调用本方法前，先检查订单是否可以被取消（如：是否已支付）
     *
     * @param requestParam 取消订单请求参数，包含订单号
     * @return true 表示订单取消成功
     * @throws ServiceException 订单不存在、订单状态不正确、更新失败时抛出
     * @throws ClientException 获取分布式锁失败时抛出（订单正在被处理，可能是重复取消）
     */
    @Override
    public Boolean cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {
        // 获取订单号
        String orderSn = requestParam.getOrderSn();
        
        // 查询订单是否存在
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn);
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        
        // 校验订单状态
        // 订单不存在，抛出异常
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } 
        // 订单状态不是"待支付"，不能取消（只能取消待支付的订单）
        else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        
        // 获取分布式锁，防止并发取消同一订单
        // 锁的Key：order:canal:order_sn_{订单号}
        RLock lock = redissonClient.getLock(StrBuilder.create("order:canal:order_sn_").append(orderSn).toString());
        // 尝试获取锁，如果获取失败（返回false），说明订单正在被其他线程处理
        // 可能是用户重复点击取消按钮，或系统正在自动关闭订单
        if (!lock.tryLock()) {
            throw new ClientException(OrderCanalErrorCodeEnum.ORDER_CANAL_REPETITION_ERROR);
        }
        
        try {
            // 更新订单主表状态为"已取消"
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(OrderStatusEnum.CLOSED.getStatus());  // 状态：已取消（30）
            LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, orderSn);
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            // 校验更新结果，如果更新行数<=0，说明更新失败（可能订单已被其他线程修改）
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
            
            // 批量更新订单明细表状态为"已取消"
            // 更新该订单下所有订单明细的状态
            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(OrderItemStatusEnum.CLOSED.getStatus());  // 订单明细状态：已取消
            LambdaUpdateWrapper<OrderItemDO> updateItemWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderSn);
            int updateItemResult = orderItemMapper.update(updateOrderItemDO, updateItemWrapper);
            // 校验更新结果，如果更新行数<=0，说明更新失败
            if (updateItemResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
        } finally {
            // 释放分布式锁，确保锁一定会被释放（即使发生异常）
            lock.unlock();
        }
        
        // 返回成功标识
        return true;
    }

    /**
     * 订单状态反转
     * 
     * 业务场景：
     * 用于订单状态的灵活修改，用于以下场景：
     * 1. 订单补偿：当订单状态异常时，需要手动修正订单状态
     * 2. 状态回滚：当某些操作失败后，需要将订单状态回滚到之前的状态
     * 3. 特殊业务处理：某些特殊业务场景需要直接修改订单状态
     * 
     * 业务规则：
     * - 只能修改"待支付"状态的订单
     * - 已支付、已完成、已取消的订单不能通过此方法修改状态
     * - 可以同时修改订单主表和订单明细表的状态
     * - 状态值由调用方指定，不进行业务校验（需要调用方保证状态值的正确性）
     * 
     * 并发控制：
     * - 使用分布式锁（Redisson）防止同一订单被并发修改状态
     * - 锁的Key格式：order:status-reversal:order_sn_{订单号}
     * - 如果获取锁失败，只记录警告日志，不抛出异常（允许并发场景下的幂等性处理）
     * 
     * 与其他方法的区别：
     * - closeTicketOrder/cancelTicketOrder：固定将状态改为"已取消"，使用事务
     * - statusReversal：可以修改为任意状态，不使用事务，获取锁失败时不抛异常
     * 
     * 注意事项：
     * - 本方法不进行业务状态校验，调用方需要确保状态值的正确性
     * - 建议仅在特殊场景下使用（如：补偿、回滚、数据修复等）
     * - 获取锁失败时不会抛出异常，但也不会执行状态更新（保证幂等性）
     * - 状态更新失败时会抛出异常，需要调用方处理
     *
     * @param requestParam 订单状态反转请求参数，包含：
     *                    - orderSn：订单号
     *                    - orderStatus：订单主表的目标状态
     *                    - orderItemStatus：订单明细表的目标状态
     * @throws ServiceException 订单不存在、订单状态不正确、更新失败时抛出
     */
    @Override
    public void statusReversal(OrderStatusReversalDTO requestParam) {
        // 查询订单是否存在
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        OrderDO orderDO = orderMapper.selectOne(queryWrapper);
        
        // 校验订单状态
        // 订单不存在，抛出异常
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } 
        // 订单状态不是"待支付"，不能修改（只能修改待支付状态的订单）
        else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        
        // 获取分布式锁，防止并发修改同一订单的状态
        // 锁的Key：order:status-reversal:order_sn_{订单号}
        RLock lock = redissonClient.getLock(StrBuilder.create("order:status-reversal:order_sn_").append(requestParam.getOrderSn()).toString());
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
            int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
            // 校验更新结果，如果更新行数<=0，说明更新失败
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
            
            // 批量更新订单明细表状态为目标状态（由请求参数指定）
            // 更新该订单下所有订单明细的状态
            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(requestParam.getOrderItemStatus());  // 使用请求参数中的目标状态
            LambdaUpdateWrapper<OrderItemDO> updateItemWrapper = Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, requestParam.getOrderSn());
            int updateItemResult = orderItemMapper.update(updateOrderItemDO, updateItemWrapper);
            // 校验更新结果，如果更新行数<=0，说明更新失败
            if (updateItemResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
        } finally {
            // 释放分布式锁，确保锁一定会被释放（即使发生异常）
            // 注意：如果之前获取锁失败，这里也会尝试释放（Redisson会处理这种情况）
            lock.unlock();
        }
    }

    /**
     * 支付结果回调订单处理
     * 
     * 业务场景：
     * 当用户完成支付后，支付服务会通过消息队列发送支付结果回调事件
     * 本方法用于接收支付回调事件，更新订单的支付信息（支付时间、支付方式）
     * 
     * 业务规则：
     * - 只更新订单的支付时间和支付方式，不更新订单状态
     * - 订单状态的更新由其他方法处理（如：支付成功后更新为"已支付"状态）
     * - 根据订单号精确匹配更新订单记录
     * 
     * 更新内容：
     * - payTime：支付时间（从支付回调事件中获取）
     * - payType：支付方式/支付渠道（如：支付宝、微信、银行卡等）
     * 
     * 注意事项：
     * - 本方法不进行订单存在性校验，如果订单不存在，更新结果为0会抛出异常
     * - 本方法不进行订单状态校验，任何状态的订单都可以更新支付信息
     * - 建议在调用本方法前，先确认订单存在且支付回调信息正确
     * - 如果更新失败（更新行数<=0），会抛出异常，需要调用方处理
     * 
     * 调用时机：
     * - 通常由消息队列消费者调用，处理支付服务发送的支付结果回调事件
     * - 支付成功后，支付服务会发送包含支付时间、支付渠道等信息的回调事件
     *
     * @param requestParam 支付结果回调事件，包含：
     *                    - orderSn：订单号
     *                    - gmtPayment：支付时间
     *                    - channel：支付渠道/支付方式
     *                    - 其他支付相关信息（如：交易号、支付金额等，本方法暂不使用）
     * @throws ServiceException 订单更新失败时抛出（订单不存在或更新失败）
     */
    @Override
    public void payCallbackOrder(PayResultCallbackOrderEvent requestParam) {
        // 构建订单更新对象，设置支付时间和支付方式
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setPayTime(requestParam.getGmtPayment());  // 支付时间（从支付回调事件中获取）
        updateOrderDO.setPayType(requestParam.getChannel());      // 支付方式/支付渠道（如：支付宝、微信等）
        
        // 构建更新条件：根据订单号精确匹配
        LambdaUpdateWrapper<OrderDO> updateWrapper = Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn());
        
        // 执行更新操作
        int updateResult = orderMapper.update(updateOrderDO, updateWrapper);
        
        // 校验更新结果，如果更新行数<=0，说明更新失败（可能是订单不存在）
        if (updateResult <= 0) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }

    /**
     * 根据订单状态类型构建对应的订单状态列表
     * 用于订单分页查询时，根据前端传入的状态类型筛选对应的订单状态
     *
     * @param requestParam 订单分页查询请求参数，包含状态类型（statusType）
     *                     0: 待支付订单
     *                     1: 已支付/退款订单（包括已支付、部分退款、全额退款）
     *                     2: 已完成订单
     * @return 订单状态码列表，用于数据库查询条件
     */
    private List<Integer> buildOrderStatusList(TicketOrderPageQueryReqDTO requestParam) {
        List<Integer> result = new ArrayList<>();
        switch (requestParam.getStatusType()) {
            // 状态类型 0：待支付订单
            case 0 -> result = ListUtil.of(
                    OrderStatusEnum.PENDING_PAYMENT.getStatus()
            );
            // 状态类型 1：已支付/退款订单（包括已支付、部分退款、全额退款）
            case 1 -> result = ListUtil.of(
                    OrderStatusEnum.ALREADY_PAID.getStatus(),
                    OrderStatusEnum.PARTIAL_REFUND.getStatus(),
                    OrderStatusEnum.FULL_REFUND.getStatus()
            );
            // 状态类型 2：已完成订单
            case 2 -> result = ListUtil.of(
                    OrderStatusEnum.COMPLETED.getStatus()
            );
        }
        return result;
    }

}
