package org.openzjl.index12306.biz.ticketservice.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
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
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_REGION_STATION_LIST;
import static org.openzjl.index12306.biz.ticketservice.common.constant.RedisKeyConstant.REGION_STATION;

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

    @Override
    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO regionStationParam) {
        String key; // 声明用于缓存的 key 变量

        // 检查查询参数中 'name' 字段是否有效 (不为空白或 null)
        if (StrUtil.isNotBlank(regionStationParam.getName())) {

            // 构造缓存 Key
            // 使用常量 REGION_STATION 作为前缀，加上查询名称作为 key
            key = REGION_STATION + regionStationParam.getName();

            return safeGetRegionStation(
                    key, // 缓存 key

                    // 定义缓存加载器（CacheLoader）：当缓存未命中时执行的数据库查询逻辑
                    () -> {
                        /**
                         * SQL 生成方式,匹配方向,匹配示例
                         * "likeRight(列, 值)",列 LIKE '值%',右模糊匹配（前缀匹配）,匹配以 '值' 开头的数据。
                         * "likeLeft(列, 值)",列 LIKE '%值',左模糊匹配（后缀匹配）,匹配以 '值' 结尾的数据。
                         */
                        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                                // 条件 1: 站名（name）以用户输入开始的模糊查询 (likeRight -> LIKE 'name%')
                                .likeRight(StationDO::getName, regionStationParam.getName())
                                .or() // 使用 OR 连接下一个条件
                                // 条件 2: 拼音（spell）以用户输入开始的模糊查询 (likeRight -> LIKE 'spell%')
                                .likeRight(StationDO::getSpell, regionStationParam.getName());

                        List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);

                        // 将 DO (Data Object) 列表转换为 DTO (Data Transfer Object) 列表
                        List<RegionStationQueryRespDTO> dtoList = BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class);

                        // 将 DTO 列表序列化为 JSON 字符串，作为缓存值返回
                        return JSON.toJSONString(dtoList);
                    },

                    // 4. 传递查询参数 (可能用于分布式锁的 key 构造)
                    regionStationParam.getName()
            );
        }
        key = REGION_STATION + regionStationParam.getQueryType();
        LambdaQueryWrapper<RegionDO> queryWrapper = switch (regionStationParam.getQueryType()) {
            case 0 -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
            case 1 -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getInitial, );
        }
        return List.of();
    }

    @Override
    public List<StationQueryRespDTO> listAllStation() {
        return List.of();
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
