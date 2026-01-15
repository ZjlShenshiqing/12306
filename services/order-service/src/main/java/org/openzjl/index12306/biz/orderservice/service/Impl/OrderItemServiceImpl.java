package org.openzjl.index12306.biz.orderservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.orderservice.dao.entity.OrderItemDO;
import org.openzjl.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import org.openzjl.index12306.biz.orderservice.dto.req.TicketOrderItemQueryReqDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderItemService;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
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
}
