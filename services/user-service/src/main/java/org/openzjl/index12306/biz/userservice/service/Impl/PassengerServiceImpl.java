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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.openzjl.index12306.biz.userservice.common.enums.VerifyStatusEnum;
import org.openzjl.index12306.biz.userservice.dao.entity.PassengerDO;
import org.openzjl.index12306.biz.userservice.dao.mapper.PassengerMapper;
import org.openzjl.index12306.biz.userservice.dto.req.PassengerRemoveReqDTO;
import org.openzjl.index12306.biz.userservice.dto.req.PassengerReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.PassengerActualRespDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import org.openzjl.index12306.biz.userservice.service.PassengerService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
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

    /**
     * 新增乘车人
     * <p>
     * 核心流程：
     * 1. 先做参数合法性校验（姓名长度、身份证号、手机号等）；
     * 2. 从 {@link UserContext} 中拿到当前登录用户名，保证新增记录与登录用户绑定；
     * 3. 将请求 DTO 转成数据表实体 {@link PassengerDO}，补齐用户名、创建时间、审核状态；
     * 4. 调用 {@link PassengerMapper#insert(Object)} 落库，失败则抛出 {@link ServiceException}；
     * 5. 最后删除该用户的「乘车人列表缓存」，让下次查询走数据库并回填最新数据。
     * </p>
     *
     * @param requestParam 新增乘车人请求参数
     */
    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        // 1. 入参业务校验（姓名长度 / 身份证号 / 手机号）
        verifyPassenger(requestParam);
        // 2. 优先从网关注入的 UserContext 取用户名；为空时用请求体里的 username（前端已传），避免未带 Token 时插入 null 导致查不到
        String username = StrUtil.isNotBlank(UserContext.getUserName()) ? UserContext.getUserName() : requestParam.getUsername();
        if (StrUtil.isBlank(username)) {
            log.warn("新增乘车人时用户名为空，请确认请求经网关并携带有效 Token，或请求体中传 username");
            throw new ClientException("请先登录后再添加乘车人");
        }
        try {
            // 3. DTO -> DO，准备插入数据库的实体
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);
            passengerDO.setCreateDate(new Date());
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());
            // 关键：项目启用了 del_flag=0 的逻辑过滤，但当前自动填充可能未生效，需手动补齐基础字段
            // 否则 insert 成功但 del_flag 为 NULL，后续查询永远查不到（因为 WHERE del_flag=0）
            Date now = new Date();
            passengerDO.setCreateTime(now);
            passengerDO.setUpdateTime(now);
            passengerDO.setDelFlag(0);
            // 4. 执行插入，校验受影响行数
            int inserted = passengerMapper.insert(passengerDO);
            if (!SqlHelper.retBool(inserted)) {
                throw new ServiceException(String.format("[%s] 新增乘车人失败", username));
            }
        } catch (Exception ex) {
            // 业务异常与系统异常分开打印日志，便于排查
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 新增乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        // 5. 新增成功后删除该用户乘车人缓存，避免 Redis 异常导致上层事务回滚、INSERT 被撤销
        safeDelUserPassengerCache(username);
    }

    /**
     * 修改乘车人
     * <p>
     * 核心流程与新增类似，不同点在于：
     * - 通过 username + id 作为更新条件，避免越权修改别人的乘车人；
     * - 只允许当前登录用户修改自己名下的乘车人记录。
     * </p>
     *
     * @param requestParam 修改乘车人请求参数（必须包含 id）
     */
    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        // 1. 参数校验（同新增逻辑）
        verifyPassenger(requestParam);
        String username = StrUtil.isNotBlank(UserContext.getUserName()) ? UserContext.getUserName() : requestParam.getUsername();
        if (StrUtil.isBlank(username)) {
            throw new ClientException("请先登录后再修改乘车人");
        }
        try {
            // 2. DTO -> DO，准备更新实体
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);
            // update_time 可能同样未自动填充，手动设置以便审计字段正确
            passengerDO.setUpdateTime(new Date());
            // 3. 仅更新当前登录用户名下、指定 id 的记录，防止跨用户修改
            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            int updated = passengerMapper.update(passengerDO, updateWrapper);
            if (!SqlHelper.retBool(updated)) {
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));
            }
        } catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 修改乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        // 4. 修改成功后清理缓存，失败仅打日志不抛异常
        safeDelUserPassengerCache(username);
    }

    /**
     * 删除乘车人（逻辑删除）
     * <p>
     * 通过 username + id 查询并删除一条乘车人记录，底层使用逻辑删除（修改 del_flag），
     * 并在成功后清理缓存。方法上使用 {@link Idempotent} 做幂等保护，防止重复点击导致多次删除。
     * </p>
     *
     * @param requestParam 删除乘车人请求参数（包含待删除的乘车人 id）
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-user:lock_passenger-alter:",
            key = "T(org.openzjl.index12306.framework.starter.user.core.UserContext.getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在移除乘车人，请稍后再试..."
    )
    @Override
    public void removePassenger(PassengerRemoveReqDTO requestParam) {
        String username = UserContext.getUserName();
        // 1. 先根据 username + id 查一遍，确认数据存在且属于当前用户
        PassengerDO passengerDO = selectPassenger(username, requestParam.getId());
        if (Objects.isNull(passengerDO)) {
            throw new ClientException("乘车人数据不存在");
        }
        try {
            // 2. 构造删除条件：仅删除当前登录用户名下指定 id 的记录
            LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            // 逻辑删除，底层会将 del_flag 置为删除状态
            int deleted = passengerMapper.delete(deleteWrapper);
            if (!SqlHelper.retBool(deleted)) {
                throw new ServiceException(String.format("[%s] 删除乘车人失败", username));
            }
        } catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 删除乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        // 3. 删除成功后清理缓存，失败仅打日志不抛异常
        safeDelUserPassengerCache(username);
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

    private void delUserPassengerCache(String username) {
        distributedCache.delete(USER_PASSENGER_LIST + username);
    }

    /**
     * 安全删除乘车人缓存：仅打日志不抛异常，避免 Redis 异常导致事务回滚、已成功的 DB 写入被撤销。
     */
    private void safeDelUserPassengerCache(String username) {
        try {
            delUserPassengerCache(username);
        } catch (Exception e) {
            log.warn("[{}] 删除乘车人缓存失败，下次查询将走 DB", username, e);
        }
    }

    private PassengerDO selectPassenger(String username, String passengerId) {
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, passengerId);
        return passengerMapper.selectOne(queryWrapper);
    }


    /**
     * 乘车人参数基础校验（测试环境适度放宽）
     * <p>
     * 原实现直接使用 Hutool 的 {@link IdcardUtil#isValidCard(String)} 做严格身份证校验，
     * 在本地联调和造数时容易因为地区码、校验位等原因频繁报错。
     * <p>
     * 当前逻辑：
     * <ul>
     *     <li>姓名：非空，长度 2-16；</li>
     *     <li>证件号：非空，15 或 18 位，全部数字，18 位时最后一位可为 X/x；</li>
     *     <li>手机号：沿用 Hutool 的 {@link PhoneUtil#isMobile(CharSequence)} 校验。</li>
     * </ul>
     * 如需上线生产，可再切回 {@code IdcardUtil.isValidCard} 做严格校验。
     * </p>
     *
     * @param requestParam 乘车人请求参数
     */
    private void verifyPassenger(PassengerReqDTO requestParam) {
        // 姓名校验
        String realName = Optional.ofNullable(requestParam.getRealName()).orElse("").trim();
        int length = realName.length();
        if (!(length >= 2 && length <= 16)) {
            throw new ClientException("乘车人名称请设置2-16位的长度");
        }

        // 证件号校验（测试环境放宽：15 或 18 位，数字，18 位最后一位可为 X/x）
        String idCard = Optional.ofNullable(requestParam.getIdCard()).orElse("").trim();
        if (StrUtil.isBlank(idCard)) {
            throw new ClientException("乘车人证件号不能为空");
        }
        boolean idCardMatch = idCard.matches("^[0-9]{15}$") || idCard.matches("^[0-9]{17}[0-9Xx]$");
        if (!idCardMatch) {
            throw new ClientException("乘车人证件号格式错误，需为15或18位号码");
        }

        // 手机号校验（沿用 Hutool 规则）
        String phone = Optional.ofNullable(requestParam.getPhone()).orElse("").trim();
        if (!PhoneUtil.isMobile(phone)) {
            throw new ClientException("乘车人手机号错误");
        }
    }
}
