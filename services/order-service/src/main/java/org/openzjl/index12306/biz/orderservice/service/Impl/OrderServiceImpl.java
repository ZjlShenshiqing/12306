package org.openzjl.index12306.biz.orderservice.service.Impl;

import cn.crane4j.annotation.AutoOperate;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderDO;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderMapper;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderService;
import org.openzjl.index12306.framework.starter.convention.page.PageResponse;
import org.openzjl.index12306.framework.starter.database.toolkit.PageUtil;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
