/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.common.enums.RegionStationQueryTypeEnum;
import org.openzjl.index12306.biz.ticketservice.dao.entity.RegionDO;
import org.openzjl.index12306.biz.ticketservice.dao.entity.StationDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.StationMapper;
import org.openzjl.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.RegionStationService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.cache.core.CacheLoader;
import org.openzjl.index12306.framework.starter.cache.toolkit.CacheUtil;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.log.enums.FlagEnum;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.openzjl.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * 地区车站接口实现层
 *
 * @author zhangjlk
 * @date 2025/11/25 15:11
 */
@Service
@RequiredArgsConstructor
public class RegionServiceImpl implements RegionStationService {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    /**
     * 按条件查询地区 / 车站列表
     *
     * 规则说明：
     * 1）如果传了 name（车站名或拼音前缀）：
     *      - 直接走站点表 StationDO，按 name / spell 前缀模糊匹配
     *      - 缓存 key = REGION_STATION + name
     * 2）如果没传 name，则按照 queryType（0~5）：
     *      - 0：热门地区（popularFlag = TRUE）
     *      - 1~5：按首字母区间（A_E、F_J...）筛 RegionDO.initial
     *      - 缓存 key = REGION_STATION + queryType
     *
     * @param regionStationParam 查询入参：
     *                           name      - 车站名称或拼音前缀（可空）
     *                           queryType - 查询类型（0 热门；1~5 按字母段）
     * @return 地区 / 车站返回列表
     */
    @Override
    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO regionStationParam) {

        // 统一缓存 key 变量
        String key;

        // 1. 如果用户传了 name（优先按 name 搜索）
        if (StrUtil.isNotBlank(regionStationParam.getName())) {

            // 构造缓存 key：按 name 前缀缓存
            key = REGION_STATION + regionStationParam.getName();

            // 通过安全封装的缓存访问方法查询
            return safeGetRegionStation(
                    key,
                    // CacheLoader：当缓存未命中时执行的逻辑（从站点表按 name / spell 模糊查询）
                    () -> {
                        /*
                         * LambdaQueryWrapper 条件说明：
                         *  - likeRight(col, value) 对应 SQL: col LIKE 'value%'（前缀匹配）
                         *  - 这里 name 和 spell 都做前缀匹配，名字或拼音开头符合都返回
                         */
                        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                                // 条件 1：站名 name 以输入前缀开头
                                .likeRight(StationDO::getName, regionStationParam.getName())
                                .or()
                                // 条件 2：拼音 spell 以输入前缀开头
                                .likeRight(StationDO::getSpell, regionStationParam.getName());

                        // 真正查库
                        List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);

                        // DO → DTO 转换
                        List<RegionStationQueryRespDTO> dtoList =
                                BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class);

                        // 返回 JSON 字符串给缓存层，safeGetRegionStation 内部会做反序列化
                        return JSON.toJSONString(dtoList);
                    },
                    // 这里把 name 作为“业务参数”，通常用于分布式锁/埋点等
                    regionStationParam.getName()
            );
        }

        // 2. 未传 name，按 queryType 做分段查询
        // 构造缓存 key：按 queryType 缓存
        key = REGION_STATION + regionStationParam.getQueryType();

        // 根据 queryType 构造不同的 RegionDO 查询条件
        LambdaQueryWrapper<RegionDO> queryWrapper = switch (regionStationParam.getQueryType()) {

            // 0：热门地区（热门标记为 TRUE）
            case 0 -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());

            // 1：首字母 in [A, B, C, D, E]
            case 1 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());

            // 2：首字母 in [F, G, H, J]
            case 2 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());

            // 3：首字母 in [K, L, M, N, O]
            case 3 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());

            // 4：首字母 in [P, Q, R, S, T]
            case 4 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());

            // 5：首字母 in [U, V, X, Y, Z]
            case 5 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());

            // 其他值一律视为非法参数
            default -> throw new ClientException("查询失败，请检查查询参数是否正确");
        };

        // 同样通过 safeGetRegionStation 做“查缓存 + 回源 + 反序列化”
        return safeGetRegionStation(
                key,
                () -> {
                    // 真正查 Region 表
                    List<RegionDO> regionDOList = regionMapper.selectList(queryWrapper);

                    // DO → DTO，并序列化为 JSON 字符串交给缓存
                    return JSON.toJSONString(
                            BeanUtil.convert(regionDOList, RegionStationQueryRespDTO.class)
                    );
                },
                // 这里把 queryType 作为“业务参数”，与上面 name 的用法类似
                String.valueOf(regionStationParam.getQueryType())
        );
    }


    /**
     * 查询所有车站列表（带分布式缓存）
     *
     * 逻辑说明：
     *  1. 先从分布式缓存中，根据固定 key：STATION_ALL 取值；
     *  2. 如果缓存命中，直接反序列化成 List 返回；
     *  3. 如果缓存未命中，则执行 Loader从数据库拉取全量车站，
     *     转成 DTO 列表后写入缓存，有效期为 ADVANCE_TICKET_DAY 天；
     *  4. 最终返回 List<StationQueryRespDTO>。
     */
    @Override
    public List<StationQueryRespDTO> listAllStation() {
        return distributedCache.safeGet(
                // 1. 缓存 key：全量车站列表的统一缓存键
                STATION_ALL,

                // 2. 反序列化的目标类型：这里用 List.class。
                //    实际上返回的是 List<StationQueryRespDTO>，外层会做泛型转换。
                List.class,

                // 3. CacheLoader：当缓存不存在/已过期时执行的回源逻辑
                () -> {
                    // 从车站表查询所有记录（emptyWrapper() = 无条件查询）
                    // 返回 List<StationDO>
                    List<StationDO> stationDOList = stationMapper.selectList(Wrappers.emptyWrapper());

                    // DO → DTO 转换：把数据对象转换为对外暴露的查询响应对象
                    return BeanUtil.convert(stationDOList, StationQueryRespDTO.class);
                },

                // 4. 缓存有效期数值：多少“天”
                ADVANCE_TICKET_DAY,

                // 5. 缓存有效期单位：这里是“天”
                TimeUnit.DAYS
        );
    }


    /**
     * 安全地从缓存中获取区域站点列表，使用双重检查锁（DCL）模式防止缓存击穿。
     *
     * @param key    缓存键
     * @param loader 缓存加载器（用于从数据源加载数据）
     * @param param  查询参数（用于构造锁的 key）
     * @return 区域站点列表（RegionStationQueryRespDTO）
     */
    private List<RegionStationQueryRespDTO> safeGetRegionStation(final String key, CacheLoader<String> loader, String param) {
        List<RegionStationQueryRespDTO> result;

        // ===========================================
        // 第一次检查：非锁定状态下的快速缓存读取（Fast Path）
        // ===========================================

        if (CollUtil.isNotEmpty( // 第一层判断：判断列表是否非空
                result = JSON.parseArray( // 将解析结果赋值给 result 变量
                        distributedCache.get(key, String.class), // 从分布式缓存中，根据 key 获取 JSON 字符串
                        RegionStationQueryRespDTO.class // 将 JSON 字符串解析成目标 DTO 对象的列表
                )
        )) {
            // 缓存命中且数据有效，直接返回，避免加锁，提升性能。
            return result;
        }

        // ===========================================
        // 缓存未命中：尝试获取分布式锁，进行数据加载
        // ===========================================

        // 构造分布式锁的 Key
        String lockKey = String.format(LOCK_QUERY_REGION_STATION_LIST, param);
        RLock lock = redissonClient.getLock(lockKey);

        // 尝试加锁。在高并发下，只有一个线程能成功加锁。
        lock.lock();
        try {
            // ===========================================
            // 第二次检查：加锁后的缓存读取（Double Check）
            // ===========================================

            if (CollUtil.isEmpty( // 【第二层判断】加锁后再次检查缓存是否为空
                    result = JSON.parseArray( // 将解析结果赋值给 result 变量
                            distributedCache.get(key, String.class), // 从分布式缓存中，再次获取 JSON 字符串
                            RegionStationQueryRespDTO.class // 将 JSON 字符串解析成目标 DTO 对象的列表
                    )
            )) {
                // 加锁后仍然发现缓存是空的：
                // 此时，当前线程是唯一可以加载数据的线程。

                // 调用 loadAndSet 方法从数据源加载数据，并写入缓存。
                if (CollUtil.isEmpty(result = loadAndSet(key, loader))) {
                    // 如果 loadAndSet 返回空，说明数据源没有数据，直接返回空列表。
                    return Collections.emptyList();
                }
                // 如果 loadAndSet 成功加载数据，result 变量已经被更新。
            }
            // 如果第二次检查发现缓存已被其它线程（之前持有锁的线程）填充，
            // 则跳过加载步骤，result 变量持有缓存中的最新数据。

        } finally {
            lock.unlock();
        }

        // 返回最终结果：
        // 如果第一次检查命中，直接返回了。
        // 如果第二次检查命中，返回缓存中的最新数据。
        // 如果执行了 loadAndSet，返回新加载的数据。
        return result;
    }

    /**
     * 从加载器加载数据，如果数据有效则写入分布式缓存，并返回解析后的数据列表。
     * 这种方法通常在缓存未命中时被调用。
     */
    private List<RegionStationQueryRespDTO> loadAndSet(final String key, CacheLoader<String> loader) {
        // 1. 从数据源（通过加载器）获取原始数据
        // loader.load() 会从数据库、远程服务等获取数据，返回一个 JSON 字符串
        String result = loader.load();

        // 2. 检查加载的数据是否为空或空白
        // 如果数据无效，则直接返回空列表，不进行缓存写入。
        if (CacheUtil.isNullOrBlank(result)) {
            return Collections.emptyList();
        }

        // 3. 将 JSON 字符串解析成 DTO 对象的列表
        // 准备返回给调用方的最终数据结构。
        List<RegionStationQueryRespDTO> respDTOList = JSON.parseArray(result, RegionStationQueryRespDTO.class);

        // 4. 将有效数据写入分布式缓存
        distributedCache.put(
                key, // 缓存的键
                result, // 要缓存的原始 JSON 字符串值
                ADVANCE_TICKET_DAY, // 缓存的有效时长（时间数值）
                TimeUnit.DAYS // 有效时长的单位（例如：天）
        );

        // 5. 返回解析后的数据列表
        return respDTOList;
    }
}
