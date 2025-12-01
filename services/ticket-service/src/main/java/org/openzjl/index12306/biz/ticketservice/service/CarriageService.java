package org.openzjl.index12306.biz.ticketservice.service;

import java.util.List;

/**
 * 列车车厢接口层
 *
 * @author zhangjlk
 * @date 2025/11/22 16:29
 */
public interface CarriageService {

    /**
     * 查询列车车厢号集合
     *
     * @param trainId       列车Id
     * @param carriageType  车厢类型
     * @return              车厢号集合
     */
    List<String> listCarriageNumber(String trainId, Integer carriageType);
}
