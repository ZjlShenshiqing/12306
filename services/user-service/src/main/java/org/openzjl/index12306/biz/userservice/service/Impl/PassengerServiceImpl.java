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
import org.openzjl.index12306.biz.userservice.common.enums.VerifyStatusEnum;
import org.openzjl.index12306.biz.userservice.dao.entity.PassengerDO;
import org.openzjl.index12306.biz.userservice.dao.mapper.PassengerMapper;
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
import org.openzjl.index12306.framework.starter.user.core.UserContext;
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
     * 保存乘车人信息
     * <p>
     * 新增乘车人信息，实现乘车人信息的持久化存储。
     * 执行流程包括参数验证、数据转换、数据库插入和缓存更新。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>验证乘车人信息的有效性（调用 {@link #verifyPassenger} 方法）</li>
     *     <li>从用户上下文获取当前用户名</li>
     *     <li>将请求参数转换为乘车人实体对象</li>
     *     <li>设置乘车人基本信息（用户名、创建时间、审核状态）</li>
     *     <li>执行数据库插入操作</li>
     *     <li>验证插入结果，失败则抛出服务异常</li>
     *     <li>删除用户的乘车人缓存，确保下次查询获取最新数据</li>
     * </ol>
     *
     * @param requestParam 乘车人请求参数对象，包含乘车人的详细信息
     * @throws ClientException 如果乘车人信息验证失败
     * @throws ServiceException 如果保存乘车人信息过程中出现服务端错误
     */
    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        // 验证乘车人信息的有效性
        verifyPassenger(requestParam);
        // 从用户上下文获取当前用户名
        String username = UserContext.getUserName();
        
        try {
            // 将请求参数转换为乘车人实体对象
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            // 设置乘车人所属用户名
            passengerDO.setUsername(username);
            // 设置创建时间
            passengerDO.setCreateDate(new Date());
            // 设置审核状态为已审核
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());
            
            // 执行数据库插入操作
            int inserted = passengerMapper.insert(passengerDO);
            // 验证插入结果
            if (!SqlHelper.retBool(inserted)) {
                throw new ServiceException(String.format("[%s] 新增乘车人失败", username));
            }
        } catch (Exception e) {
            // 异常处理
            if (e instanceof ServiceException) {
                log.error("{}，请求参数：{}", e.getMessage(), JSON.toJSONString(requestParam));
            } else {
                // 修复异常变量名错误：将ex改为e
                log.error("[{}] 新增乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), e);
            }
            throw e;
        }
        
        // 删除用户的乘车人缓存，确保下次查询获取最新数据
        delUserPassengerCache(username);
    }

    /**
     * 更新乘车人信息
     * <p>
     * 更新已有的乘车人信息，实现乘车人信息的持久化更新。
     * 该方法确保只有乘车人所属用户才能进行更新操作，通过用户名和ID双条件约束保证数据安全性。
     * </p>
     *
     * @param requestParam 乘车人请求参数对象，必须包含乘车人ID和待更新的详细信息
     * @throws ClientException 如果乘车人信息验证失败
     * @throws ServiceException 如果更新乘车人信息过程中出现服务端错误或更新失败
     */
    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        // 验证乘车人信息的有效性
        verifyPassenger(requestParam);
        
        // 从用户上下文获取当前用户名
        String username = UserContext.getUserName();
        
        try {
            // 将请求参数转换为乘车人实体对象
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            // 确保乘车人归属于当前用户
            passengerDO.setUsername(username);
            
            // 构建更新条件：用户名和乘车人ID双条件约束，防止越权操作
            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            
            // 执行数据库更新操作
            int updated = passengerMapper.update(passengerDO, updateWrapper);
            
            // 验证更新结果
            if (!SqlHelper.retBool(updated)) {
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));
            }
        } catch (Exception ex) {
            // 异常处理
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 修改乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        
        // 删除用户的乘车人缓存，确保下次查询获取最新数据
        delUserPassengerCache(username);
    }

    /**
     * 移除乘车人信息
     * <p>
     * 删除指定的乘车人信息，实现乘车人信息的持久化删除。
     * 该方法确保只有乘车人所属用户才能进行删除操作，通过用户名和ID双条件约束保证数据安全性。
     * </p>
     *
     * <p><strong>幂等性保障：</strong></p>
     * <ul>
     *     <li>使用 {@link Idempotent} 注解确保方法幂等性</li>
     *     <li>幂等键：基于当前用户名生成唯一键</li>
     *     <li>场景：REST API 接口调用</li>
     *     <li>重复请求提示：正在移除乘车人，请稍后再尝试...</li>
     * </ul>
     *
     * @param requestParam 乘车人请求参数对象，必须包含乘车人ID
     * @throws ClientException 如果乘车人不存在或请求参数无效
     * @throws ServiceException 如果删除乘车人信息过程中出现服务端错误或删除失败
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-user:lock_passenger-alter:",
            key = "T(org.openzjl.index12306.framework.starter.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在移除乘车人，请稍后再尝试..."
    )
    @Override
    public void removePassenger(PassengerReqDTO requestParam) {
        // 从用户上下文获取当前用户名
        String username = UserContext.getUserName();
        
        // 根据用户名和乘车人ID查询乘车人信息
        PassengerDO passengerDO = selectPassenger(username, requestParam.getId());
        
        // 验证乘车人是否存在
        if (Objects.isNull(passengerDO)) {
            throw new ClientException("乘车人数据不存在");
        }
        
        try {
            // 构建删除条件：用户名和乘车人ID双条件约束，防止越权操作
            LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            
            // 执行数据库删除操作
            int deleted = passengerMapper.delete(deleteWrapper);
            
            // 验证删除结果
            if (!SqlHelper.retBool(deleted)) {
                throw new ServiceException(String.format("[%s] 删除乘车人失败", username));
            }
        } catch (Exception ex) {
            // 异常处理
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 删除乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            throw ex;
        }
        
        // 删除用户的乘车人缓存，确保下次查询获取最新数据
        delUserPassengerCache(username);
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

    /**
     * 根据用户名和乘车人ID查询乘车人信息
     * <p>
     * 从数据库中查询指定用户名和乘车人ID的乘车人信息，确保数据归属正确。
     * 如果查询不到符合条件的记录，返回 null。
     * </p>
     *
     * @param username    用户名（用于校验乘车人归属）
     * @param passengerId 乘车人ID（唯一标识乘车人的主键）
     * @return 乘车人实体对象，如果查询不到则返回 null
     */
    private PassengerDO selectPassenger(String username, String passengerId) {
        // 构建查询条件：用户名和乘车人ID精确匹配
        LambdaQueryWrapper<PassengerDO> queryWrapper = Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .eq(PassengerDO::getId, passengerId);
        // 执行查询并返回结果
        return passengerMapper.selectOne(queryWrapper);
    }
    
    /**
     * 删除用户的乘车人缓存
     * <p>
     * 当用户的乘车人信息发生变更（添加、修改、删除）时，删除对应的缓存数据，
     * 确保下次查询时能获取到最新的乘车人信息。
     * </p>
     *
     * @param username 用户名（用于构建缓存Key）
     */
    private void delUserPassengerCache(String username) {
        // 构建缓存Key并删除缓存
        // 使用USER_PASSENGER_LIST常量拼接用户名作为缓存Key
        distributedCache.delete(USER_PASSENGER_LIST + username);
    }

    /**
     * 验证乘车人信息的有效性
     * <p>
     * 对乘车人请求参数进行多维度验证，确保数据符合业务规则和格式要求。
     * 验证通过则继续执行，验证失败则抛出客户端异常。
     * </p>
     *
     * @param requestParam 乘车人请求参数对象，包含需要验证的乘车人信息
     * @throws ClientException 如果验证失败，抛出客户端异常，包含具体的错误信息
     */
    private void verifyPassenger(PassengerReqDTO requestParam) {
        // 验证乘车人姓名长度：2-16个字符
        int length = requestParam.getRealName().length();
        if (!(length >= 2 && length <= 16)) {
            throw new ClientException("乘车人名称请设置2-16位的长度");
        }
        // 验证身份证号格式合法性
        if (!IdcardUtil.isValidCard(requestParam.getIdCard())) {
            throw new ClientException("乘车人证件号错误");
        }
        // 验证手机号格式合法性
        if (!PhoneUtil.isMobile(requestParam.getPhone())) {
            throw new ClientException("乘车人手机号错误");
        }
    }
}
