/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.userservice.service.Impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.userservice.common.enums.UserChainMarkEnum;
import org.openzjl.index12306.biz.userservice.dao.entity.*;
import org.openzjl.index12306.biz.userservice.dao.mapper.*;
import org.openzjl.index12306.biz.userservice.dto.req.UserDeletionReqDTO;
import org.openzjl.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import org.openzjl.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserLoginRespDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import org.openzjl.index12306.biz.userservice.dto.resp.UserRegisterRespDTO;
import org.openzjl.index12306.biz.userservice.service.UserLoginService;
import org.openzjl.index12306.biz.userservice.service.UserService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.openzjl.index12306.framework.starter.user.core.UserInfoDTO;
import org.openzjl.index12306.framework.starter.user.toolkit.JWTUtil;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.openzjl.index12306.biz.userservice.common.constant.RedisKeyConstant.*;
import static org.openzjl.index12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum.*;
import static org.openzjl.index12306.biz.userservice.toolkit.UserReuseUtil.hashShardingIdx;

/**
 * 用户登录接口实现
 *
 * @author zhangjlk
 * @date 2026/1/7 16:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    private final UserMailMapper userMailMapper;
    private final UserPhoneMapper userPhoneMapper;
    private final UserMapper userMapper;
    private final DistributedCache distributedCache;
    private final RBloomFilter<String> cachePenetrationBloomFilter;
    private final AbstractChainContext abstractChainContext;
    private final RedissonClient redissonClient;
    private final UserReuseMapper userReuseMapper;
    private final UserService userService;
    private final UserDeletionMapper userDeletionMapper;

    /**
     * 用户登录接口实现
     * <p>
     * 支持多种登录方式：用户名、邮箱、手机号登录
     * 登录成功后生成 JWT Token 并缓存用户信息，用于后续请求的身份验证。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>识别登录方式：判断输入是邮箱、手机号还是用户名（通过检查是否包含 '@' 符号）。</li>
     *     <li>查询用户名映射：如果是邮箱或手机号，从对应表中查询关联的用户名。</li>
     *     <li>用户验证：使用用户名和密码查询用户表，验证账号和密码是否正确。</li>
     *     <li>生成 Token：验证成功后，生成 JWT 访问令牌。</li>
     *     <li>缓存用户信息：将用户信息和 Token 存入缓存，有效期 30 分钟。</li>
     *     <li>返回登录结果：返回用户信息和 Token。</li>
     * </ol>
     *
     * <p>支持的登录方式：</p>
     * <ul>
     *     <li>用户名登录：直接使用用户名和密码登录。</li>
     *     <li>邮箱登录：输入邮箱地址，系统自动查找关联的用户名。</li>
     *     <li>手机号登录：输入手机号，系统自动查找关联的用户名。</li>
     * </ul>
     *
     * @param requestParam 登录请求参数（包含用户名/邮箱/手机号、密码）
     * @return 登录响应对象（包含用户ID、用户名、真实姓名、访问令牌）
     * @throws ClientException 当邮箱不存在时抛出
     * @throws ServiceException 当账号不存在或密码错误时抛出
     */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        // 获取登录凭证
        // 用户可能输入用户名、邮箱或手机号，需要识别具体类型
        String usernameOrEmailOrPhone = requestParam.getUsernameOrMailOrPhone();
        
        // 识别登录方式（邮箱/手机号/用户名）
        // 通过检查是否包含 '@' 符号来判断是否为邮箱
        // 如果包含 '@'，则为邮箱；否则可能是手机号或用户名
        boolean mailFlag = false;
        for (char c : usernameOrEmailOrPhone.toCharArray()) {
            if (c == '@') {
                mailFlag = true;
                break;  // 找到 '@' 符号后立即退出循环
            }
        }

        // 根据登录方式查询用户名映射
        String username;
        if (mailFlag) {
            // 邮箱登录
            // 从邮箱表中查询该邮箱关联的用户名
            LambdaQueryWrapper<UserMailDO> queryWrapper = Wrappers.lambdaQuery(UserMailDO.class)
                    .eq(UserMailDO::getMail, usernameOrEmailOrPhone);  // 根据邮箱查询
            
            // 查询邮箱记录，提取用户名
            // 如果邮箱不存在，抛出异常（邮箱必须预先绑定）
            username = Optional.ofNullable(userMailMapper.selectOne(queryWrapper))
                    .map(UserMailDO::getUsername)  // 提取用户名
                    .orElseThrow(() -> new ClientException("用户名/手机号/邮箱不存在"));
        } else {
            // 手机号登录/用户名登录
            // 从手机号表中查询该手机号关联的用户名
            LambdaQueryWrapper<UserPhoneDO> queryWrapper = Wrappers.lambdaQuery(UserPhoneDO.class)
                    .eq(UserPhoneDO::getPhone, usernameOrEmailOrPhone);  // 根据手机号查询
            
            // 查询手机号记录，提取用户名
            // 如果手机号不存在，返回 null（可能是直接使用用户名登录）
            username = Optional.ofNullable(userPhoneMapper.selectOne(queryWrapper))
                    .map(UserPhoneDO::getUsername)  // 提取用户名
                    .orElse(null);
        }
        
        // 确定最终用户名
        // 如果通过邮箱或手机号查询到了用户名，使用查询到的用户名
        // 如果查询不到（可能是直接使用用户名登录），使用原始输入作为用户名
        username = Optional.ofNullable(username).orElse(usernameOrEmailOrPhone);
        
        // 用户验证（用户名 + 密码） =
        // 使用用户名和密码查询用户表，验证账号和密码是否正确
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username)                    // 用户名匹配
                .eq(UserDO::getPassword, requestParam.getPassword()) // 密码匹配
                .select(UserDO::getId, UserDO::getUsername, UserDO::getRealName);  // 只查询需要的字段，提高性能
        
        // 执行查询，如果用户名和密码都正确，返回用户信息
        UserDO userDO = userMapper.selectOne(queryWrapper);
        
        // 验证成功，生成 Token 并缓存
        if (userDO != null) {
            // 构建用户信息DTO（用于生成 Token 和返回给前端）
            UserInfoDTO userInfo = UserInfoDTO.builder()
                    .userId(String.valueOf(userDO.getId()))      // 用户ID（转换为字符串）
                    .username(userDO.getUsername())              // 用户名
                    .realName(userDO.getRealName())              // 真实姓名
                    .build();

            // 生成 JWT 访问令牌
            // Token 中包含用户ID、用户名、真实姓名等信息，用于后续请求的身份验证
            String accessToken = JWTUtil.generateAccessToken(userInfo);
            
            // 构建登录响应对象
            UserLoginRespDTO userLogin = UserLoginRespDTO.builder()
                    .userId(userInfo.getUserId())        // 用户ID
                    .username(userInfo.getUsername())    // 用户名
                    .realName(userInfo.getRealName())    // 真实姓名
                    .accessToken(accessToken)            // 访问令牌
                    .build();
            
            // 将用户信息和 Token 存入缓存，有效期 30 分钟
            // Key: accessToken（Token 本身作为 key）
            // Value: 用户登录信息的 JSON 字符串
            // 用途：后续请求可以通过 Token 快速获取用户信息，无需查询数据库
            distributedCache.put(accessToken, JSON.toJSONString(userLogin), 30, TimeUnit.MINUTES);
            
            // 返回登录结果
            return userLogin;
        }
        
        // 验证失败，抛出异常
        // 如果用户名或密码错误，抛出异常
        throw new ServiceException("账号不存在或密码错误");
    }

    /**
     * 检查登录状态
     * <p>
     * 根据访问令牌（Token）从缓存中获取用户登录信息，用于验证用户是否已登录
     * 如果 Token 有效且未过期，返回用户信息；如果 Token 不存在或已过期，返回 null
     * </p>
     *
     * @param accessToken 访问令牌（JWT Token）
     * @return 用户登录信息，如果 Token 无效或已过期则返回 null
     */
    @Override
    public UserLoginRespDTO checkLogin(String accessToken) {
        // 从缓存中获取用户登录信息
        // Key: accessToken（Token 本身作为 key）
        // Value: UserLoginRespDTO 对象（包含用户ID、用户名、真实姓名等信息）
        // 如果 Token 不存在或已过期，返回 null
        return distributedCache.get(accessToken, UserLoginRespDTO.class);
    }

    /**
     * 用户登出接口
     * <p>
     * 删除缓存中的用户登录信息，使 Token 失效，用户需要重新登录才能访问需要身份验证的接口。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>校验 Token 是否为空：如果为空，直接返回，无需删除。</li>
     *     <li>删除缓存：从缓存中删除该 Token 对应的用户信息，使 Token 失效。</li>
     * </ol>
     *
     * <p>注意事项：</p>
     * <ul>
     *     <li>登出后，该 Token 将无法再用于身份验证。</li>
     *     <li>即使 Token 未过期，登出后也会立即失效。</li>
     *     <li>如果 Token 为空或已过期，不会抛出异常，静默处理。</li>
     * </ul>
     *
     * @param accessToken 访问令牌（JWT Token）
     */
    @Override
    public void logout(String accessToken) {
        // 校验 Token 是否为空
        // 如果为空，直接返回，无需删除（避免无效操作）
        if (StrUtil.isNotBlank(accessToken)) {
            // 从缓存中删除该 Token 对应的用户信息
            // 删除后，该 Token 将无法再用于身份验证，用户需要重新登录
            distributedCache.delete(accessToken);
        }
    }

    /**
     * 检查用户名是否已存在
     * <p>
     * 使用布隆过滤器 + Redis Set 双重检查机制，快速判断用户名是否已被注册。
     * 这种设计可以防止缓存穿透，同时保证查询性能
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>布隆过滤器检查：快速判断用户名"可能存在"。</li>
     *     <li>Redis Set 精确检查：如果布隆过滤器返回 true，从 Redis Set 中精确查询。</li>
     *     <li>返回结果：如果布隆过滤器返回 false，直接返回 true（用户名不存在）。</li>
     * </ol>
     *
     * <p>设计说明：</p>
     * <ul>
     *     <li>布隆过滤器：快速过滤，如果返回 false，说明用户名一定不存在（无假阴性）</li>
     *     <li>Redis Set：精确查询，如果布隆过滤器返回 true，需要进一步确认（可能有误判）。</li>
     *     <li>分片存储：使用哈希分片将用户名分散到多个 Set 中，避免单个 Set 过大。</li>
     * </ul>
     *
     * @param username 用户名
     * @return {@code true} 表示用户名不存在（可以注册），{@code false} 表示用户名已存在（不能注册）
     */
    @Override
    public Boolean hasUserName(String username) {
        // 布隆过滤器快速检查
        // 使用布隆过滤器快速判断用户名"可能存在"
        // - 如果返回 false：说明用户名一定不存在（无假阴性），可以直接返回 true
        // - 如果返回 true：说明用户名可能存在（可能有误判），需要进一步查询 Redis Set 确认
        boolean hasUserName = cachePenetrationBloomFilter.contains(username);
        
        if (hasUserName) {
            // Redis Set 精确检查
            // 如果布隆过滤器返回 true，需要从 Redis Set 中精确查询
            // 因为布隆过滤器可能有误判（假阳性），需要进一步确认
            
            // 获取 Redis 操作模板
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            
            // 构建 Redis Set 的 key
            // 使用哈希分片：将用户名通过哈希函数映射到不同的 Set 中，避免单个 Set 过大
            // 格式：USER_REGISTER_REUSE_SHARDING + 分片索引
            // 例如：user_register_reuse_sharding_0、user_register_reuse_sharding_1 等
            String shardingKey = USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username);
            
            // 从 Redis Set 中检查用户名是否存在
            // 如果存在，返回 false（用户名已存在，不能注册）
            // 如果不存在，返回 true（用户名不存在，可以注册）
            return instance.opsForSet().isMember(shardingKey, username);
        }
        
        // 布隆过滤器返回 false，直接返回
        // 如果布隆过滤器返回 false，说明用户名一定不存在（无假阴性）
        // 直接返回 true，无需查询 Redis，提高性能
        return true;
    }

    /**
     * 用户注册接口实现
     * <p>
     * 支持用户注册功能，包括用户名、手机号、邮箱的注册和关联。
     * 使用分布式锁保证并发安全，使用事务保证数据一致性。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>责任链校验：参数校验、用户名/手机号/邮箱是否已存在等。</li>
     *     <li>获取分布式锁：基于用户名获取锁，防止并发注册同一用户名。</li>
     *     <li>插入用户主表：创建用户基本信息记录。</li>
     *     <li>插入手机号表：关联手机号到用户名。</li>
     *     <li>插入邮箱表：如果提供了邮箱，关联邮箱到用户名。</li>
     *     <li>清理复用数据：从用户名复用表和缓存中删除该用户名。</li>
     *     <li>更新布隆过滤器：将用户名添加到布隆过滤器中。</li>
     *     <li>释放锁：无论成功与否，都要释放分布式锁。</li>
     * </ol>
     *
     * @param requestParam 用户注册请求参数（包含用户名、密码、手机号、邮箱等）
     * @return 用户注册响应对象（包含注册成功的用户信息）
     * @throws ServiceException 当用户名/手机号/邮箱已存在或注册失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserRegisterRespDTO register(UserRegisterReqDTO requestParam) {
        // 责任链校验
        // 执行用户注册前置校验（参数校验、用户名/手机号/邮箱是否已存在等）
        // 确保请求合法后再继续后续流程
        abstractChainContext.handler(UserChainMarkEnum.USER_REGISTER_FILTER.name(), requestParam);
        
        // 获取分布式锁
        // 基于用户名获取分布式锁，防止并发注册同一用户名
        // 锁的 key：LOCK_USER_REGISTER + 用户名
        // 例如：lock_user_register:zhangsan
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER + requestParam.getUsername());
        
        // 尝试获取锁，如果获取不到（说明其他线程正在注册该用户名），抛出异常
        // 使用 tryLock() 非阻塞方式，避免线程等待
        boolean tryLock = lock.tryLock();
        if (!tryLock) {
            throw new ServiceException(HAS_USERNAME_NOTNULL);
        }
        
        try {
            // 插入用户主表
            // 将注册请求参数转换为用户实体对象，插入用户主表
            try {
                // 使用 BeanUtil 将 DTO 转换为实体对象
                int inserted = userMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
                
                // 检查插入结果，如果插入失败（返回 0），抛出异常
                if (inserted < 1) {
                    throw new ServiceException(USER_REGISTER_FAIL);
                }
            } catch (DuplicateKeyException duplicateKeyException) {
                // 捕获唯一键冲突异常（用户名已存在）
                // 记录错误日志，然后抛出业务异常
                log.error("用户名 [{}] 重复注册", requestParam.getUsername());
                throw new ServiceException(HAS_USERNAME_NOTNULL);
            }
            
            // 插入手机号表
            // 构建手机号实体对象，关联手机号到用户名
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(requestParam.getPhone())              // 手机号
                    .username(requestParam.getUsername())        // 用户名
                    .build();
            
            try {
                // 插入手机号表，建立手机号与用户名的映射关系
                userPhoneMapper.insert(userPhoneDO);
            } catch (DuplicateKeyException duplicateKeyException) {
                // 捕获唯一键冲突异常（手机号已被其他用户注册）
                // 记录错误日志，然后抛出业务异常
                log.error("用户 [{}] 注册手机号 [{}] 重复", requestParam.getUsername(), requestParam.getPhone());
                throw new ServiceException(PHONE_REGISTERED);
            }
            
            // 插入邮箱表
            // 如果用户提供了邮箱，则关联邮箱到用户名
            if (StrUtil.isNotBlank(requestParam.getMail())) {
                // 构建邮箱实体对象，关联邮箱到用户名
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(requestParam.getMail())                // 邮箱
                        .username(requestParam.getUsername())       // 用户名
                        .build();
                
                try {
                    // 插入邮箱表，建立邮箱与用户名的映射关系
                    userMailMapper.insert(userMailDO);
                } catch (DuplicateKeyException duplicateKeyException) {
                    // 捕获唯一键冲突异常（邮箱已被其他用户注册）
                    // 记录错误日志，然后抛出业务异常
                    log.error("用户 [{}] 注册邮箱 [{}] 重复", requestParam.getUsername(), requestParam.getMail());
                    throw new ServiceException(MAIL_REGISTERED);
                }
            }
            
            // 清理用户名复用数据
            // 如果该用户名之前被注销过（存储在复用表中），现在重新注册，需要清理复用数据
            String username = requestParam.getUsername();
            
            // 从用户名复用表中删除该用户名
            // 表示该用户名已被重新使用，不再可复用
            userReuseMapper.delete(Wrappers.update(new UserReuseDO(username)));
            
            // 从 Redis Set 中删除该用户名
            // 使用哈希分片，根据用户名计算分片索引，从对应的 Set 中删除
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().remove(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
            
            // 更新布隆过滤器
            // 将新注册的用户名添加到布隆过滤器中
            // 这样后续查询用户名是否存在时，可以快速判断（防止缓存穿透）
            cachePenetrationBloomFilter.add(username);
        } finally {
            lock.unlock();
        }

        // 返回注册结果
        // 将注册请求参数转换为响应对象，返回给前端
        return BeanUtil.convert(requestParam, UserRegisterRespDTO.class);
    }

    /**
     * 用户注销接口实现。
     * <p>
     * 用户注销功能，用于删除用户账号及相关数据，并将用户名添加到复用表供后续注册使用。
     * 使用分布式锁保证并发安全，使用事务保证数据一致性。
     * </p>
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>校验用户身份：确保当前登录用户与要注销的用户一致，防止恶意注销他人账号。</li>
     *     <li>获取分布式锁：基于用户名获取锁，防止并发注销操作。</li>
     *     <li>查询用户信息：获取用户的完整信息（身份证、手机号、邮箱等）。</li>
     *     <li>插入注销记录表：记录用户的身份证信息，用于后续防止重复注册。</li>
     *     <li>更新用户主表：设置注销时间，标记用户已注销（软删除）。</li>
     *     <li>更新手机号表：设置注销时间，标记手机号已注销。</li>
     *     <li>更新邮箱表：如果存在邮箱，设置注销时间，标记邮箱已注销。</li>
     *     <li>清理缓存：删除缓存中的 Token，使用户无法再使用该 Token 登录。</li>
     *     <li>添加到复用表：将用户名添加到复用表，供后续注册使用。</li>
     *     <li>添加到 Redis Set：将用户名添加到 Redis Set（分片存储），用于快速查询。</li>
     *     <li>释放锁：无论成功与否，都要释放分布式锁。</li>
     * </ol>
     *
     * <p>数据表操作：</p>
     * <ul>
     *     <li>用户注销表（UserDeletionDO）：记录已注销用户的身份证信息，防止同一身份证重复注册。</li>
     *     <li>用户主表（UserDO）：软删除，设置注销时间，不物理删除数据。</li>
     *     <li>手机号表（UserPhoneDO）：软删除，设置注销时间。</li>
     *     <li>邮箱表（UserMailDO）：软删除，设置注销时间。</li>
     *     <li>用户名复用表（UserReuseDO）：存储可复用的用户名。</li>
     * </ul>
     *
     * <p>注意事项：</p>
     * <ul>
     *     <li>使用 {@code @Transactional} 注解，任何步骤失败都会回滚所有数据库操作。</li>
     *     <li>使用软删除机制，不物理删除数据，保留历史记录。</li>
     *     <li>注销后用户名可以复用，但需要等待一定时间（由业务规则决定）。</li>
     *     <li>同一身份证号不能重复注册（通过注销表校验）。</li>
     * </ul>
     *
     * @param requestParam 用户注销请求参数（包含用户名）
     * @throws ServiceException 当用户身份验证失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletion(UserDeletionReqDTO requestParam) {
        // 校验用户身份，从 UserContext 获取当前登录的用户名
        // 确保只有用户本人才能注销自己的账号，防止恶意注销他人账号
        String username = UserContext.getUserName();

        // 比较当前登录用户名和要注销的用户名是否一致
        // 如果不一致，抛出异常，拒绝注销操作
        if (!Objects.equals(username, requestParam.getUsername())) {
            throw new ServiceException("注销账号与登录账号不一致");
        }

        // 基于用户名获取分布式锁，防止并发注销操作
        // 锁的 key：USER_DELETION + 用户名
        // 例如：user_deletion:zhangsan
        RLock lock = redissonClient.getLock(USER_DELETION + requestParam.getUsername());
        lock.lock();
        try {
            // 查询用户信息
            // 查询用户的完整信息，包括身份证、手机号、邮箱等
            // 这些信息用于后续的注销操作和记录
            UserQueryRespDTO userQueryRespDTO = userService.queryUserByUsername(username);
            
            // 插入注销记录表
            // 记录已注销用户的身份证信息，用于后续防止同一身份证重复注册
            // 这是业务规则：同一身份证号不能重复注册
            UserDeletionDO userDeletionDO = UserDeletionDO.builder()
                    .idType(userQueryRespDTO.getIdType())      // 证件类型
                    .idCard(userQueryRespDTO.getIdCard())       // 身份证号
                    .build();
            userDeletionMapper.insert(userDeletionDO);
            
            // 更新用户主表
            // 设置注销时间，标记用户已注销
            // 使用软删除机制，不物理删除数据，保留历史记录
            UserDO userDO = new UserDO();
            userDO.setDeletionTime(System.currentTimeMillis());  // 设置注销时间（当前时间戳）
            userDO.setUsername(username);                         // 用户名
            userMapper.deletionUser(userDO);
            
            // 更新手机号表
            // 设置手机号的注销时间，标记手机号已注销
            // 这样该手机号可以重新绑定到其他用户名
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(userQueryRespDTO.getPhone())              // 手机号
                    .deletionTime(System.currentTimeMillis())        // 设置注销时间
                    .build();
            userPhoneMapper.deletionUser(userPhoneDO);
            
            // 更新邮箱表
            // 如果用户有邮箱，设置邮箱的注销时间，标记邮箱已注销
            // 这样该邮箱可以重新绑定到其他用户名
            if (StrUtil.isNotBlank(userQueryRespDTO.getMail())) {
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(userQueryRespDTO.getMail())            // 邮箱
                        .deletionTime(System.currentTimeMillis())    // 设置注销时间
                        .build();
                userMailMapper.deletionUser(userMailDO);
            }
            
            // 清理缓存中的 Token
            // 删除缓存中的用户登录信息，使 Token 失效
            // 用户无法再使用该 Token 进行身份验证，需要重新登录
            distributedCache.delete(UserContext.getToken());
            
            // 将用户名添加到复用表
            // 将用户名添加到复用表，供后续注册使用
            // 这样其他用户可以使用该用户名进行注册
            userReuseMapper.insert(new UserReuseDO(username));
            
            // 将用户名添加到 Redis Set
            // 将用户名添加到 Redis Set 中，用于快速查询用户名是否可复用
            // 使用哈希分片，根据用户名计算分片索引，添加到对应的 Set 中
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().add(USER_REGISTER_REUSE_SHARDING + hashShardingIdx(username), username);
        } finally {
            // 释放分布式锁，无论注销成功与否，都要释放锁，避免死锁
            lock.unlock();
        }
    }
}
