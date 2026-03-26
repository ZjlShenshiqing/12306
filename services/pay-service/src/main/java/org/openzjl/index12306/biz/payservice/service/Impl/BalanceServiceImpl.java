/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.service.Impl;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.payservice.dto.resp.BalanceInfoRespDTO;
import org.openzjl.index12306.biz.payservice.service.BalanceService;
import org.openzjl.index12306.framework.starter.bases.constant.UserConstant;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.openzjl.index12306.framework.starter.user.core.UserInfoDTO;
import org.openzjl.index12306.framework.starter.user.toolkit.JWTUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.openzjl.index12306.biz.payservice.common.constant.RedisKeyConstant.USER_BALANCE_INFO;

/**
 * 閻劍鍩涙担娆擃杺閺堝秴濮熺€圭偟骞?
 */
@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private final DistributedCache distributedCache;

    private final RedissonClient redissonClient;

    @Override
    public BalanceInfoRespDTO queryCurrentUserBalance() {
        String username = getCurrentUsername();
        return buildBalanceInfo(username, getBalanceCent(username));
    }

    @Override
    public BalanceInfoRespDTO recharge(BigDecimal amount) {
        int amountCent = toCent(amount);
        if (amountCent <= 0) {
            throw new ServiceException("recharge amount must be greater than 0");
        }
        String username = getCurrentUsername();
        String cacheKey = buildBalanceCacheKey(username);
        RLock lock = redissonClient.getLock(StrBuilder.create("pay:balance:lock:").append(username).toString());
        lock.lock();
        try {
            int current = getBalanceCent(username);
            int latest = current + amountCent;
            distributedCache.put(cacheKey, latest);
            return buildBalanceInfo(username, latest);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BalanceInfoRespDTO pay(BigDecimal amount) {
        int amountCent = toCent(amount);
        if (amountCent <= 0) {
            throw new ServiceException("pay amount must be greater than 0");
        }
        String username = getCurrentUsername();
        String cacheKey = buildBalanceCacheKey(username);
        RLock lock = redissonClient.getLock(StrBuilder.create("pay:balance:lock:").append(username).toString());
        lock.lock();
        try {
            int current = getBalanceCent(username);
            if (current < amountCent) {
                throw new ServiceException("insufficient balance, please recharge first");
            }
            int latest = current - amountCent;
            distributedCache.put(cacheKey, latest);
            return buildBalanceInfo(username, latest);
        } finally {
            lock.unlock();
        }
    }

    private String getCurrentUsername() {
        String username = UserContext.getUserName();
        if (StrUtil.isNotBlank(username)) {
            return username;
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            username = request.getHeader(UserConstant.USER_NAME_KEY);
            if (StrUtil.isBlank(username)) {
                username = request.getHeader("x-user-name");
            }
            if (StrUtil.isBlank(username)) {
                String authorization = request.getHeader("Authorization");
                UserInfoDTO userInfoDTO = JWTUtil.parseJwtToken(authorization);
                if (userInfoDTO != null) {
                    username = userInfoDTO.getUsername();
                }
            }
        }

        if (StrUtil.isBlank(username)) {
            throw new ServiceException("cannot get current login user, please pass Authorization or username header");
        }
        return username;
    }

    private int getBalanceCent(String username) {
        Integer balanceCent = distributedCache.get(buildBalanceCacheKey(username), Integer.class);
        return balanceCent == null ? 0 : balanceCent;
    }

    private String buildBalanceCacheKey(String username) {
        return String.format(USER_BALANCE_INFO, username);
    }

    private BalanceInfoRespDTO buildBalanceInfo(String username, int balanceCent) {
        return BalanceInfoRespDTO.builder()
                .username(username)
                .balance(fromCent(balanceCent))
                .build();
    }

    private int toCent(BigDecimal amount) {
        return amount.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal fromCent(int cent) {
        return new BigDecimal(cent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
