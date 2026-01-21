package org.openzjl.index12306.biz.orderservice.service.Impl;

import cn.crane4j.annotation.AutoOperate;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderDO;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderMapper;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderItemCreateReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderSelfPageQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.mq.event.DelayCloseOrderEvent;
import org.openzjl.index12306.biz.orderservice.mq.produce.DelayCloseOrderSendProduce;
import org.openzjl.index12306.biz.orderservice.remote.UserRemoteService;
import org.openzjl.index12306.biz.orderservice.remote.dto.UserQueryActualRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.biz.orderservice.service.OrderPassengerRelationService;
import org.openzjl.index12306.biz.orderservice.service.OrderService;
import org.openzjl.index12306.biz.orderservice.service.orderid.OrderIdGeneratorManager;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.convention.page.PageResponse;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.database.toolkit.PageUtil;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.springframework.stereotype.Service;

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
