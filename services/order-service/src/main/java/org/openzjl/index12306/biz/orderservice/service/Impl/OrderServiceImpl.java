package org.openzjl.index12306.biz.orderservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderDO;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderMapper;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderService;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

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


}
