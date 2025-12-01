package org.openzjl.index12306.biz.ticketservice.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.service.CarriageService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 列车车厢接口层实现
 *
 * @author zhangjlk
 * @date 2025/12/1 10:26
 */
@Service
@RequiredArgsConstructor
public class CarriageServiceImpl implements CarriageService {

    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    @Override
    public List<String> listCarriageNumber(String trainId, Integer carriageType) {
        return List.of();
    }
}
