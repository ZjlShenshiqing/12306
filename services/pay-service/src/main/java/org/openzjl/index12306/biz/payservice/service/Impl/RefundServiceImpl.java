package org.openzjl.index12306.biz.payservice.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.alipay.api.domain.AlipayCommerceIotDapplyRefundCreateModel;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.payservice.common.enums.TradeStatusEnum;
import org.openzjl.index12306.biz.payservice.convert.RefundRequestConvert;
import org.openzjl.index12306.biz.payservice.dao.entity.PayDO;
import org.openzjl.index12306.biz.payservice.dao.entity.RefundDO;
import org.openzjl.index12306.biz.payservice.dao.mapper.PayMapper;
import org.openzjl.index12306.biz.payservice.dao.mapper.RefundMapper;
import org.openzjl.index12306.biz.payservice.dto.RefundCreateDTO;
import org.openzjl.index12306.biz.payservice.dto.base.RefundRequest;
import org.openzjl.index12306.biz.payservice.dto.base.RefundResponse;
import org.openzjl.index12306.biz.payservice.dto.command.RefundCommand;
import org.openzjl.index12306.biz.payservice.dto.req.RefundReqDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.RefundRespDTO;
import org.openzjl.index12306.biz.payservice.mq.event.RefundResultCallBackOrderEvent;
import org.openzjl.index12306.biz.payservice.mq.produce.RefundResultCallbackOrderSendProduce;
import org.openzjl.index12306.biz.payservice.remote.TicketOrderRemoteService;
import org.openzjl.index12306.biz.payservice.remote.dto.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.payservice.service.RefundService;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractStrategyChoose;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * 退款接口层实现
 *
 * @author zhangjlk
 * @date 2026/1/27 12:29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {


    private final PayMapper payMapper;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final RefundMapper refundMapper;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final RefundResultCallbackOrderSendProduce refundResultCallbackOrderSendProduce;

    /**
     * 通用退款方法
     * <p>
     * 处理各种支付渠道的退款请求，支持完整的退款流程，包括：
     * 1. 查询支付单并验证存在性
     * 2. 计算退款后的剩余支付金额
     * 3. 创建退款单记录
     * 4. 构建退款命令并转换为具体渠道的退款请求
     * 5. 使用策略模式执行退款操作
     * 6. 更新支付单状态
     * 7. 准备更新退款单状态
     * </p>
     * 
     * <p><strong>事务管理：</strong></p>
     * <ul>
     *   <li>使用 @Transactional 注解确保整个退款过程的事务一致性</li>
     *   <li>如果任何步骤失败，整个事务会回滚，确保数据一致性</li>
     * </ul>
     *
     * @param requestParam 退款请求参数
     *                     <ul>
     *                       <li>包含订单号（orderSn）：用于查询支付单</li>
     *                       <li>包含退款金额（refundAmount）：本次退款的金额</li>
     *                       <li>包含退款详情列表（refundDetailReqDTOList）：每个乘客的退款详情</li>
     *                     </ul>
     * @return 退款响应对象
     *         <ul>
     *           <li>目前返回 null，待实现完整的响应构建逻辑</li>
     *           <li>未来应返回包含退款结果、退款单号等信息的响应对象</li>
     *         </ul>
     * @throws ServiceException 如果支付单不存在或退款处理过程中出现异常
     *                          <ul>
     *                            <li>"支付单不存在"：根据订单号未找到对应的支付记录</li>
     *                            <li>"修改支付单退款结果失败"：更新支付单状态失败</li>
     *                            <li>其他异常：由 createRefund 方法或策略执行过程抛出</li>
     *                          </ul>
     */
    @Override
    @Transactional
    public RefundRespDTO commonRefund(RefundReqDTO requestParam) {
        // 初始化退款响应对象
        RefundRespDTO refundRespDTO = null;
        
        // 构建查询条件：根据订单号查询支付单
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        
        // 执行数据库查询，获取支付单
        PayDO payDO = payMapper.selectOne(queryWrapper);
        
        // 验证支付单是否存在
        if (Objects.isNull(payDO)) {
            // 记录错误日志
            log.error("支付单不存在，orderSn: {}", requestParam.getOrderSn());
            // 抛出异常，触发事务回滚
            throw new ServiceException("支付单不存在");
        }
        
        // 计算剩余支付金额：原始总金额减去本次退款金额
        payDO.setPayAmount(payDO.getTotalAmount() - requestParam.getRefundAmount());
        
        // 创建退款单
        // 1. 将退款请求参数转换为退款创建DTO
        RefundCreateDTO refundCreateDTO = BeanUtil.convert(requestParam, RefundCreateDTO.class);
        // 2. 设置支付流水号（从支付单中获取）
        refundCreateDTO.setPaySn(payDO.getPaySn());
        // 3. 调用 createRefund 方法创建退款记录
        createRefund(refundCreateDTO);

        // 构建退款命令
        // 1. 将支付单转换为退款命令
        RefundCommand refundCommand = BeanUtil.convert(payDO, RefundCommand.class);
        // 2. 设置退款金额（转换为 BigDecimal 类型，确保精度）
        refundCommand.setPayAmount(new BigDecimal(requestParam.getRefundAmount()));
        
        // 转换为退款请求：根据支付渠道类型转换为对应渠道的退款请求
        RefundRequest refundRequest = RefundRequestConvert.command2RefundRequest(refundCommand);
        
        // 执行退款策略：使用策略模式选择并执行对应的退款处理逻辑
        // refundRequest.buildMark() 生成策略选择标记
        // refundRequest 作为处理参数
        RefundResponse result = abstractStrategyChoose.chooseAndExecuteResp(refundRequest.buildMark(), refundRequest);
        
        // 更新支付单状态：设置为退款执行结果的状态
        payDO.setStatus(result.getStatus());
        
        // 构建支付单更新条件：根据订单号
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, requestParam.getOrderSn());
        
        // 执行支付单更新操作
        int updateResult = payMapper.update(payDO, updateWrapper);
        
        // 验证更新结果
        if (updateResult <= 0) {
            // 抛出异常，触发事务回滚
            throw new ServiceException("修改支付单退款结果失败");
        }
        
        // 准备更新退款单状态：构建退款单更新条件
        LambdaUpdateWrapper<RefundDO> refundUpdateWrapper = Wrappers.lambdaUpdate(RefundDO.class)
                .eq(RefundDO::getOrderSn, requestParam.getOrderSn());
        RefundDO refundDO = new RefundDO();
        refundDO.setTradeNo(result.getTradeNo());
        refundDO.setStatus(result.getStatus());

        int refundUpdateResult = refundMapper.update(refundDO, refundUpdateWrapper);
        if (refundUpdateResult <= 0) {
            log.error("修改退款单退款结果失败，退款单信息: {}", JSON.toJSONString(refundDO));
        }

        // 返回退款响应
        if (Objects.equals(result.getStatus(), TradeStatusEnum.TRADE_CLOSED.tradeCode())) {
            RefundResultCallBackOrderEvent refundResultCallBackOrderEvent = RefundResultCallBackOrderEvent.builder()
                    .orderSn(requestParam.getOrderSn())
                    .refundType(requestParam.getRefundTypeEnum())
                    .partialRefundTicketDetailList(requestParam.getRefundDetailReqDTOList())
                    .build();
            refundResultCallbackOrderSendProduce.sendMessage(refundResultCallBackOrderEvent);
        }

        return refundRespDTO;
    }

    /**
     * 创建退款单
     * <p>
     * 根据退款请求参数创建退款单记录，包含以下步骤：
     * 1. 查询车票订单详情，验证订单是否存在
     * 2. 遍历退款详情列表，为每个退款项创建退款记录
     * 3. 填充退款记录的相关信息（支付流水号、订单号、车次信息、乘客信息等）
     * 4. 执行退款记录的数据库插入操作
     * </p>
     *
     * @param requestParam 退款创建请求参数
     *                     <ul>
     *                       <li>包含支付流水号（paySn）、订单号（orderSn）</li>
     *                       <li>包含退款详情列表（refundDetailReqDTOList），每个详情包含乘客信息和退款金额</li>
     *                     </ul>
     * @throws ServiceException 如果车票订单不存在或退款创建失败
     */
    private void createRefund(RefundCreateDTO requestParam) {
        // 调用远程服务查询车票订单详情，验证订单是否存在
        Result<TicketOrderDetailRespDTO> queryTicketResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(requestParam.getOrderSn());
        
        // 验证查询结果：如果查询失败或数据为空，抛出异常
        if (!queryTicketResult.isSuccess() && Objects.isNull(queryTicketResult.getData())) {
            throw new ServiceException("车票订单不存在");
        }
        
        // 获取车票订单详情数据
        TicketOrderDetailRespDTO ticketOrderDetailRespDTO = queryTicketResult.getData();
        
        // 遍历退款详情列表，为每个退款项创建退款记录
        requestParam.getRefundDetailReqDTOList().forEach(each -> {
            // 创建退款记录实体
            RefundDO refundDO = new RefundDO();
            
            // 设置支付流水号
            refundDO.setPaySn(requestParam.getPaySn());
            // 设置订单号
            refundDO.setOrderSn(requestParam.getOrderSn());
            // 设置车次ID
            refundDO.setTrainId(ticketOrderDetailRespDTO.getTrainId());
            // 设置车次号
            refundDO.setTrainNumber(ticketOrderDetailRespDTO.getTrainNumber());
            // 设置出发站
            refundDO.setDeparture(ticketOrderDetailRespDTO.getDeparture());
            // 设置到达站
            refundDO.setArrival(ticketOrderDetailRespDTO.getArrival());
            // 设置出发时间
            refundDO.setDepartureTime(ticketOrderDetailRespDTO.getDepartureTime());
            // 设置到达时间
            refundDO.setArrivalTime(ticketOrderDetailRespDTO.getArrivalTime());
            // 设置乘车日期
            refundDO.setRidingDate(ticketOrderDetailRespDTO.getRidingDate());
            // 设置座位类型
            refundDO.setSeatType(each.getSeatType());
            // 设置证件类型
            refundDO.setIdType(each.getIdType());
            // 设置证件号
            refundDO.setIdCard(each.getIdCard());
            // 设置真实姓名
            refundDO.setRealName(each.getRealName());
            // 设置退款时间（当前时间）
            refundDO.setRefundTime(new Date());
            // 设置退款金额
            refundDO.setAmount(each.getAmount());
            // 设置用户ID
            refundDO.setUserId(each.getUserId());
            // 设置用户名
            refundDO.setUsername(each.getUsername());
            
            // 执行退款记录的数据库插入操作
            refundMapper.insert(refundDO);
        });
    }
}
