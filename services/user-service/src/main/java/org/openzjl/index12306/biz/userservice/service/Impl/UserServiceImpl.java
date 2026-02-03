/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.userservice.dao.entity.UserDO;
import org.openzjl.index12306.biz.userservice.dao.entity.UserDeletionDO;
import org.openzjl.index12306.biz.userservice.dao.entity.UserMailDO;
import org.openzjl.index12306.biz.userservice.dao.mapper.UserDeletionMapper;
import org.openzjl.index12306.biz.userservice.dao.mapper.UserMailMapper;
import org.openzjl.index12306.biz.userservice.dao.mapper.UserMapper;
import org.openzjl.index12306.biz.userservice.dto.req.UserUpdateReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserQueryActualRespDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import org.openzjl.index12306.biz.userservice.service.UserService;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户信息接口实现层
 * <p>
 * 提供用户信息的查询、更新等功能，包括：
 * <ul>
 *     <li>根据用户ID或用户名查询用户信息</li>
 *     <li>查询用户注销次数（用于防止重复注册）</li>
 *     <li>更新用户基本信息（包括邮箱）</li>
 * </ul>
 * </p>
 *
 * @author zhangjlk
 * @date 2026/1/11 14:17
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 用户主表数据访问对象
     */
    private final UserMapper userMapper;
    
    /**
     * 用户注销表数据访问对象
     */
    private final UserDeletionMapper userDeletionMapper;
    
    /**
     * 用户邮箱表数据访问对象
     */
    private final UserMailMapper userMailMapper;

    /**
     * 根据用户ID查询用户信息
     * <p>
     * 通过用户ID从用户主表中查询用户信息，如果用户不存在则抛出异常
     * </p>
     *
     * @param userId 用户ID（字符串类型）
     * @return 用户查询响应对象（包含用户基本信息）
     * @throws ClientException 当用户不存在时抛出
     */
    @Override
    public UserQueryRespDTO queryUserByUserId(String userId) {
        // 构建查询条件：根据用户ID进行精确匹配
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId);
        
        // 执行查询，从用户主表中查询用户信息
        UserDO userDO = userMapper.selectOne(queryWrapper);
        
        // 校验查询结果，如果用户不存在，抛出异常
        if (userDO == null) {
            throw new ClientException("用户不存在，请检查用户ID是否正确");
        }
        
        // 将实体对象转换为响应DTO对象，返回给调用方
        return BeanUtil.convert(userDO, UserQueryRespDTO.class);
    }

    /**
     * 根据用户名查询用户信息
     * <p>
     * 通过用户名从用户主表中查询用户信息，如果用户不存在则抛出异常。
     * 该方法被其他方法（如 {@link #queryActualUserByUserName}）调用，是查询用户信息的基础方法。
     * </p>
     *
     * @param username 用户名
     * @return 用户查询响应对象（包含用户基本信息）
     * @throws ClientException 当用户不存在时抛出
     */
    @Override
    public UserQueryRespDTO queryUserByUsername(String username) {
        // 构建查询条件：根据用户名进行精确匹配
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        
        // 执行查询，从用户主表中查询用户信息
        UserDO userDO = userMapper.selectOne(queryWrapper);
        
        // 校验查询结果，如果用户不存在，抛出异常
        if (userDO == null) {
            throw new ClientException("用户不存在，请检查用户名是否正确");
        }
        
        // 将实体对象转换为响应DTO对象，返回给调用方
        return BeanUtil.convert(userDO, UserQueryRespDTO.class);
    }

    /**
     * 根据用户名查询实际用户信息。
     * <p>
     * 该方法是对 {@link #queryUserByUsername} 的封装，将查询结果转换为 {@link UserQueryActualRespDTO} 类型。
     * 主要用于返回脱敏后的敏感信息
     * </p>
     *
     * @param userName 用户名
     * @return 用户实际查询响应对象（可能包含脱敏后的敏感信息）
     * @throws ClientException 当用户不存在时抛出（由 {@link #queryUserByUsername} 抛出）
     */
    @Override
    public UserQueryActualRespDTO queryActualUserByUserName(String userName) {
        // 先调用 queryUserByUsername 查询用户信息
        // 然后将查询结果转换为 UserQueryActualRespDTO 类型
        // 该类型包含脱敏后的敏感信息
        return BeanUtil.convert(queryUserByUsername(userName), UserQueryActualRespDTO.class);
    }

    /**
     * 查询用户注销次数
     * <p>
     * 根据证件类型和证件号查询该证件对应的用户注销次数。
     * 用于业务规则校验：同一身份证号不能重复注册（或限制注册次数）。
     * </p>
     *
     * <p>场景：</p>
     * <ul>
     *     <li>用户注册时，检查该身份证号是否已经注销过多次。</li>
     *     <li>防止恶意用户反复注册和注销账号。</li>
     *     <li>如果注销次数超过限制，可以拒绝注册。</li>
     * </ul>
     *
     * @param idType 证件类型（如：身份证、护照等）
     * @param idCard 证件号（如：身份证号）
     * @return 用户注销次数，如果未查询到记录则返回 0
     */
    @Override
    public Integer queryUserDeletionNum(Integer idType, String idCard) {
        // 构建查询条件：根据证件类型和证件号进行精确匹配
        LambdaQueryWrapper<UserDeletionDO> queryWrapper = Wrappers.lambdaQuery(UserDeletionDO.class)
                .eq(UserDeletionDO::getIdType, idType)      // 证件类型匹配
                .eq(UserDeletionDO::getIdCard, idCard);     // 证件号匹配
        
        // 执行统计查询，从用户注销表中统计符合条件的记录数
        Long deletionCount = userDeletionMapper.selectCount(queryWrapper);
        
        // 将 Long 类型转换为 Integer 类型
        // 如果查询结果为 null，返回 0（表示该证件号从未注销过）
        return Optional.ofNullable(deletionCount).map(Long::intValue).orElse(0);
    }

    /**
     * 更新用户信息
     * <p>
     * 支持更新用户基本信息和邮箱信息。
     * 如果更新了邮箱，会先删除旧的邮箱记录，再插入新的邮箱记录。
     * </p>
     *
     * @param requestParam 用户更新请求参数（包含用户名、邮箱等需要更新的字段）
     * @throws ClientException 当用户不存在时抛出（由 {@link #queryUserByUsername} 抛出）
     */
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 查询用户信息，获取当前用户的完整信息
        // 如果用户不存在，queryUserByUsername 会抛出异常
        UserQueryRespDTO userQueryResult = queryUserByUsername(requestParam.getUsername());
        
        // 将查询结果转换为实体对象，用于更新操作
        UserDO user = BeanUtil.convert(userQueryResult, UserDO.class);
        
        // 构建更新条件：根据用户名进行精确匹配
        LambdaUpdateWrapper<UserDO> userUpdateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        
        // 更新用户主表，更新用户基本信息
        // 注意：这里只更新 user 对象中非 null 的字段
        userMapper.update(user, userUpdateWrapper);
        
        // 更新邮箱（如果请求中包含新邮箱且与旧邮箱不同）
        // 条件判断：
        // 1. 请求参数中的邮箱不为空
        // 2. 请求参数中的邮箱与当前用户的邮箱不同
        if (StrUtil.isNotBlank(requestParam.getMail()) && !Objects.equals(requestParam.getMail(), userQueryResult.getMail())) {
            // 构建删除条件：根据旧邮箱进行精确匹配
            LambdaUpdateWrapper<UserMailDO> updateWrapper = Wrappers.lambdaUpdate(UserMailDO.class)
                    .eq(UserMailDO::getMail, userQueryResult.getMail());
            
            // 删除旧的邮箱记录
            // 这样该邮箱可以重新绑定到其他用户名
            userMailMapper.delete(updateWrapper);
            
            // 构建新的邮箱记录
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())           // 新邮箱
                    .username(requestParam.getUsername())   // 用户名
                    .build();
            
            // 插入新的邮箱记录，建立新邮箱与用户名的映射关系
            userMailMapper.insert(userMailDO);
        }
    }
}
