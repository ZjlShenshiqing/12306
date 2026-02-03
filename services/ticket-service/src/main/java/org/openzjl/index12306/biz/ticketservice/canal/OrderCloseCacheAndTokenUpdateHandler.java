package org.openzjl.index12306.biz.ticketservice.canal;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import org.openzjl.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import org.openzjl.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.tokenbucket.TicketAvailabilityTokenBucket;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractExecuteStrategy;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单关闭缓存和令牌更新处理器
 * <p>
 * 实现 AbstractExecuteStrategy 接口，用于处理订单关闭或取消后的缓存和令牌更新操作。
 * 当订单状态变为关闭（状态码 30）时，会执行以下操作：
 * 1. 解锁座位
 * 2. 回滚令牌桶中的可用票数
 * </p>
 * 
 * <p><strong>设计用途：</strong></p>
 * <ul>
 *   <li>处理 Canal 数据同步中的订单关闭事件</li>
 *   <li>确保订单关闭后座位资源及时释放</li>
 *   <li>保证令牌桶中的可用票数与实际座位数一致</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2026/2/3 17:52
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseCacheAndTokenUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {

    /**
     * 订单远程服务
     * <p>
     * 用于查询订单详情信息
     * </p>
     */
    private final TicketOrderRemoteService ticketOrderRemoteService;
    
    /**
     * 座位服务
     * <p>
     * 用于解锁座位资源
     * </p>
     */
    private final SeatService seatService;
    
    /**
     * 票量令牌桶
     * <p>
     * 用于回滚可用票数
     * </p>
     */
    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;

    /**
     * 执行订单关闭后的处理逻辑
     * <p>
     * 处理 Canal 数据同步中的订单关闭事件，解锁座位并回滚令牌桶中的可用票数。
     * </p>
     *
     * @param message Canal 二进制日志事件
     *               <ul>
     *                 <li>包含订单数据变更信息，如订单状态、订单号等</li>
     *               </ul>
     * 
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>过滤出状态为 30（关闭）的订单数据</li>
     *   <li>如果没有关闭状态的订单数据，直接返回</li>
     *   <li>遍历关闭状态的订单数据：
     *     <ul>
     *       <li>根据订单号查询订单详情</li>
     *       <li>如果查询成功且订单详情不为空：
     *         <ul>
     *           <li>获取列车 ID、出发站、到达站和乘客详情</li>
     *           <li>调用座位服务解锁座位</li>
     *           <li>调用令牌桶服务回滚可用票数</li>
     *         </ul>
     *       </li>
     *     </ul>
     *   </li>
     * </ol>
     */
    @Override
    public void execute(CanalBinlogEvent message) {
        // 过滤出状态为 30（关闭）的订单数据
        List<Map<String, Object>> messageDataList = message.getData().stream()
                .filter(each -> each.get("status") != null)
                .filter(each -> Objects.equals(each.get("status"), "30"))
                .toList();
        
        // 如果没有关闭状态的订单数据，直接返回
        if (CollUtil.isEmpty(messageDataList)) {
            return;
        }
        
        // 遍历处理关闭状态的订单
        for (Map<String, Object> messageData : messageDataList) {
            // 根据订单号查询订单详情
            Result<TicketOrderDetailRespDTO> orderDetailResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(messageData.get("order_sn").toString());
            TicketOrderDetailRespDTO orderDetailResultData = orderDetailResult.getData();
            
            // 如果查询成功且订单详情不为空
            if (orderDetailResult.isSuccess() && orderDetailResultData != null) {
                String trainId = String.valueOf(orderDetailResultData.getTrainId());
                List<TicketOrderPassengerDetailRespDTO> passengerDetails = orderDetailResultData.getPassengerDetails();
                
                // 解锁座位
                seatService.unLock(trainId, orderDetailResultData.getDeparture(), orderDetailResultData.getArrival(), BeanUtil.convert(passengerDetails, TrainPurchaseTicketRespDTO.class));
                
                // 回滚令牌桶中的可用票数
                ticketAvailabilityTokenBucket.rollbackInBucket(orderDetailResultData);
            }
        }
    }

    /**
     * 获取策略标记
     * <p>
     * 返回订单表的实际表名，用于策略选择。
     * </p>
     *
     * @return 订单表的实际表名
     *         <ul>
     *           <li>返回值：{@link CanalExecuteStrategyMarkEnum#T_ORDER} 的实际表名</li>
     *         </ul>
     */
    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getActualTable();
    }

    /**
     * 获取模式匹配标记
     * <p>
     * 返回订单表的模式匹配表名（正则表达式），用于匹配分表场景。
     * </p>
     *
     * @return 订单表的模式匹配表名
     *         <ul>
     *           <li>返回值：{@link CanalExecuteStrategyMarkEnum#T_ORDER} 的模式匹配表名</li>
     *         </ul>
     */
    @Override
    public String patternMatchMark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getPatternMatchTable();
    }
}
