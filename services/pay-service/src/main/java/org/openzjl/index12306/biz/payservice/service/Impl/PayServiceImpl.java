/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.common.enums.TradeStatusEnum;
import org.openzjl.index12306.biz.payservice.dao.entity.PayDO;
import org.openzjl.index12306.biz.payservice.dao.mapper.PayMapper;
import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.biz.payservice.dto.base.PayResponse;
import org.openzjl.index12306.biz.payservice.dto.req.PayCallbackReqDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.PayInfoRespDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.PayRespDTO;
import org.openzjl.index12306.biz.payservice.mq.event.PayResultCallbackOrderEvent;
import org.openzjl.index12306.biz.payservice.mq.produce.PayResultCallbackOrderSendProduce;
import org.openzjl.index12306.biz.payservice.service.BalanceService;
import org.openzjl.index12306.biz.payservice.service.PayService;
import org.openzjl.index12306.biz.payservice.service.payid.PayIdGeneratorManager;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractStrategyChoose;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.openzjl.index12306.biz.payservice.common.constant.RedisKeyConstant.ORDER_PAY_RESULT_INFO;

/**
 * 闁衡偓椤栨瑧甯涢柡鍫濈Т婵喓鈧湱鍋熼獮?
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final DistributedCache distributedCache;

    private final AbstractStrategyChoose abstractStrategyChoose;

    private final PayMapper payMapper;

    private final BalanceService balanceService;

    private final PayResultCallbackOrderSendProduce payResultCallbackOrderSendProduce;

    @Idempotent(
            type = IdempotentTypeEnum.SPEL,
            uniqueKeyPrefix = "index12306-pay:lock_create_pay:",
            key = "#requestParam.getOutOrderSn()"
    )
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayRespDTO commonPay(PayRequest requestParam) {
        if (Objects.isNull(requestParam)) {
            throw new ServiceException("unsupported pay channel");
        }

        PayRespDTO cacheResult = distributedCache.get(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(), PayRespDTO.class);
        if (cacheResult != null) {
            return cacheResult;
        }

        boolean isBalancePay = Objects.equals(requestParam.getChannel(), PayChannelEnum.BALANCE_PAY.getCode());

        PayResponse result;
        if (isBalancePay) {
            result = PayResponse.builder().body("BALANCE_PAY_SUCCESS").build();
        } else {
            result = abstractStrategyChoose.chooseAndExecuteResp(requestParam.buildMark(), requestParam);
        }

        PayDO insertPay = BeanUtil.convert(requestParam, PayDO.class);
        String paySn = PayIdGeneratorManager.generateId(requestParam.getOrderSn());
        insertPay.setPaySn(paySn);
        insertPay.setStatus(TradeStatusEnum.WAIT_BUYER_PAY.tradeCode());
        Date now = new Date();
        insertPay.setCreateTime(now);
        insertPay.setUpdateTime(now);
        insertPay.setDelFlag(0);
        insertPay.setTotalAmount(requestParam.getTotalAmount().multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue());

        int insert = payMapper.insert(insertPay);
        if (insert <= 0) {
            log.error("create pay record failed, request={}", JSON.toJSONString(requestParam));
            throw new ServiceException("create pay record failed");
        }

        if (isBalancePay) {
            balanceService.pay(requestParam.getTotalAmount());

            PayDO updatePayDO = new PayDO();
            updatePayDO.setStatus(TradeStatusEnum.TRADE_SUCCESS.tradeCode());
            updatePayDO.setPayAmount(insertPay.getTotalAmount());
            updatePayDO.setTradeNo(paySn);
            updatePayDO.setGmtPayment(new Date());
            updatePayDO.setUpdateTime(new Date());
            LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                    .eq(PayDO::getPaySn, paySn);
            int updateResult = payMapper.update(updatePayDO, updateWrapper);
            if (updateResult <= 0) {
                throw new ServiceException("update pay record failed after balance deduction");
            }
            insertPay.setStatus(updatePayDO.getStatus());
            insertPay.setPayAmount(updatePayDO.getPayAmount());
            insertPay.setTradeNo(updatePayDO.getTradeNo());
            insertPay.setGmtPayment(updatePayDO.getGmtPayment());
            payResultCallbackOrderSendProduce.sendMessage(BeanUtil.convert(insertPay, PayResultCallbackOrderEvent.class));
        }

        distributedCache.put(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(), JSON.toJSONString(result), 10, TimeUnit.MINUTES);
        return BeanUtil.convert(result, PayRespDTO.class);
    }

    @Override
    public PayInfoRespDTO getPayInfoByOrderSn(String orderSn) {
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, orderSn);
        PayDO payDO = payMapper.selectOne(queryWrapper);
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }

    @Override
    public PayInfoRespDTO getPayInfoByPaySn(String paySn) {
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getPaySn, paySn);
        PayDO payDO = payMapper.selectOne(queryWrapper);
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void callbackPay(PayCallbackReqDTO payCallbackRequestParam) {
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, payCallbackRequestParam.getOrderRequestId());
        PayDO payDO = payMapper.selectOne(queryWrapper);
        if (Objects.isNull(payDO)) {
            log.error("闁衡偓椤栨瑧甯涢柛妤佹磻缁楀鈧稒锚濠€顏堟晬鐎圭湕derRequestId={}", payCallbackRequestParam.getOrderRequestId());
            throw new ServiceException("pay record not found");
        }
        payDO.setTradeNo(payCallbackRequestParam.getTradeNo());
        payDO.setStatus(TradeStatusEnum.WAIT_BUYER_PAY.tradeCode());
        payDO.setPayAmount(payCallbackRequestParam.getPayAmount());
        payDO.setGmtPayment(payCallbackRequestParam.getGmtPayment());
        LambdaUpdateWrapper<PayDO> updateWrapper = Wrappers.lambdaUpdate(PayDO.class)
                .eq(PayDO::getOrderSn, payCallbackRequestParam.getOrderSn());
        int result = payMapper.update(payDO, updateWrapper);
        if (result <= 0) {
            log.error("濞ｅ浂鍠楅弫濂稿绩椤栨瑧甯涢柛妤佹礃閺侇喗绂掑Ο鑲╂尝闁哄绮岄妵鎴犳嫻閵夘垳绀夐柡鈧娆戝笡闁告娲戞穱濠囧箒? {}", JSON.toJSONString(payDO));
        }
        if (Objects.equals(payCallbackRequestParam.getStatus(), TradeStatusEnum.TRADE_SUCCESS.tradeCode())) {
            payResultCallbackOrderSendProduce.sendMessage(BeanUtil.convert(payDO, PayResultCallbackOrderEvent.class));
        }
    }
}
