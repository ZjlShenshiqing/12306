/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.userservice.dao.entity.PassengerDO;
import org.openzjl.index12306.biz.userservice.dao.mapper.PassengerMapper;
import org.openzjl.index12306.biz.userservice.dto.req.PassengerRemoveReqDTO;
import org.openzjl.index12306.biz.userservice.dto.req.PassengerReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.PassengerActualRespDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import org.openzjl.index12306.biz.userservice.service.PassengerService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.openzjl.index12306.biz.userservice.common.constant.RedisKeyConstant.USER_PASSENGER_LIST;

/**
 * 乘车人接口实现层
 *
 * @author zhangjlk
 * @date 2026/1/11 15:47
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    /**
     * 分布式缓存客户端
     * 用于缓存乘车人信息，提高查询性能
     */
    private final DistributedCache distributedCache;
    
    /**
     * 乘车人数据访问对象
     * 用于从数据库查询乘车人信息
     */
    private final PassengerMapper passengerMapper;

    /**
     * 根据用户名查询乘车人列表
     * <p>
     * 当前实现返回空列表，需要完善实现逻辑。
     * 建议调用 {@link #getActualUserPassengerListStr} 方法获取缓存中的乘车人信息。
     * </p>
     *
     * @param username 用户名
     * @return 乘车人响应列表（当前返回空列表）
     */
    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        return Optional.ofNullable(actualUserPassengerListStr)
                .map(each -> JSON.parseArray(each, PassengerDO.class))
                .map(each -> BeanUtil.convert(each, PassengerRespDTO.class))
                .orElse(null);
    }

    /**
     * 根据用户名和乘车人ID列表查询指定的乘车人信息
     * <p>
     * 从缓存中获取用户的乘车人列表，然后根据ID列表过滤出指定的乘车人信息。
     * 返回脱敏后的乘车人信息（包含身份证号、手机号等敏感信息的脱敏版本）。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>获取缓存数据：调用 {@link #getActualUserPassengerListStr} 从缓存中获取用户的乘车人列表（JSON字符串）。</li>
     *     <li>校验数据：如果缓存数据为空，直接返回 null。</li>
     *     <li>解析JSON：将JSON字符串解析为 {@link PassengerActualRespDTO} 列表。</li>
     *     <li>过滤数据：使用Stream API过滤出ID在指定列表中的乘车人。</li>
     *     <li>数据转换：将过滤后的乘车人实体转换为响应DTO对象。</li>
     *     <li>返回结果：返回过滤后的乘车人列表。</li>
     * </ol>
     *
     * @param username 用户名（用于校验乘车人归属）
     * @param ids      乘车人ID列表（需要查询的乘车人ID集合）
     * @return 乘车人实际响应列表（包含脱敏后的敏感信息），如果用户没有乘车人或缓存数据为空则返回 null
     */
    @Override
    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        // 从缓存中获取用户的乘车人列表（JSON字符串）
        // 如果缓存中没有数据，会从数据库查询并存入缓存
        String actualUserPassengerListStr = getActualUserPassengerListStr(username);
        
        // 如果缓存数据为空，说明用户没有乘车人，直接返回 null
        if (StrUtil.isEmpty(actualUserPassengerListStr)) {
            return null;
        }
        
        // 将JSON字符串解析为 PassengerDO 列表
        // 然后使用Stream API进行过滤和转换
        return JSON.parseArray(actualUserPassengerListStr, PassengerDO.class)
                .stream()
                // 过滤出ID在指定列表中的乘车人
                // 只返回用户选择的乘车人信息
                .filter(passenger -> ids.contains(passenger.getId()))
                // 将实体对象转换为响应DTO对象
                // 这里使用 BeanUtil.convert 进行对象转换，确保数据格式正确
                .map(each -> BeanUtil.convert(each, PassengerActualRespDTO.class))
                // 收集为List并返回
                .collect(Collectors.toList());
    }

    @Override
    public void savePassenger(PassengerReqDTO requestParam) {

    }

    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {

    }

    @Override
    public void removePassenger(PassengerRemoveReqDTO requestParam) {

    }

    /**
     * 获取用户乘车人列表的 JSON 字符串（带缓存）
     * <p>
     * 使用缓存机制查询用户的乘车人列表，提高查询性能并减少数据库压力。
     * 采用"缓存穿透保护"策略：如果缓存中没有数据，则从数据库查询并存入缓存。
     * </p>
     *
     * @param username 用户名
     * @return 乘车人列表的 JSON 字符串，如果用户没有乘车人则返回 null
     */
    private String getActualUserPassengerListStr(String username) {
        // 使用 safeGet 方法实现缓存穿透保护
        return distributedCache.safeGet(
                USER_PASSENGER_LIST + username,  // 缓存 Key：用户乘车人列表
                String.class,                         // 缓存 Value 类型：JSON 字符串
                () -> {
                    // 缓存未命中时执行的查询逻辑
                    // 构建查询条件：根据用户名进行精确匹配
                    LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username);
                    
                    // 从数据库查询该用户的所有乘车人信息
                    List<PassengerDO> passengerDOList = passengerMapper.selectList(queryWrapper);
                    
                    // 如果查询结果不为空，转换为 JSON 字符串；如果为空，返回 null
                    // 注意：返回 null 时不会存入缓存，避免缓存穿透问题
                    return CollUtil.isNotEmpty(passengerDOList) ? JSON.toJSONString(passengerDOList) : null;
                },
                1,      // 缓存过期时间：1 天
                TimeUnit.DAYS   // 时间单位：天
        );
    }

    private void verifyPassenger(PassengerReqDTO requestParam) {
        int length = requestParam.getRealName().length();
        if (!(length >= 2 && length <= 16)) {
            throw new ClientException("乘车人名称请设置2-16位的长度");
        }
        if (!IdcardUtil.isValidCard(requestParam.getIdCard())) {
            throw new ClientException("乘车人证件号错误");
        }
        if (!PhoneUtil.isMobile(requestParam.getPhone())) {
            throw new ClientException("乘车人手机号错误");
        }
    }
}
