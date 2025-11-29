package org.openzjl.index12306.biz.ticketservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 列车站点接口实现层
 *
 * @author zhangjlk
 * @date 2025/11/29 09:31
 */
@Service
@RequiredArgsConstructor
public class TrainStationServiceImpl implements TrainStationService {

    private final TrainStationMapper trainStationMapper;

    @Override
    public List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId) {
        // ---------------------------------------------------------
        // 步骤 1: 构建查询条件
        // ---------------------------------------------------------
        // Wrappers.lambdaQuery() 创建一个 LambdaQueryWrapper 对象，TrainStationDO 指定了要查询的数据库实体表
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                // 这一句翻译成 SQL 就是： WHERE train_id = trainId
                .eq(TrainStationDO::getTrainId, trainId);

        // ---------------------------------------------------------
        // 步骤 2: 执行数据库查询
        // ---------------------------------------------------------
        // 调用 Mapper 接口的 selectList 方法，返回的结果是 DO (Data Object) 列表，即直接对应数据库表结构的实体对象
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        // ---------------------------------------------------------
        // 步骤 3: 对象转换 (DO -> DTO)
        // ---------------------------------------------------------
        // BeanUtil.convert 是一个工具方法
        // 它会自动把 trainStationDOList 里每个对象的属性值，复制到新的 TrainStationQueryRespDTO 对象中。
        return BeanUtil.convert(trainStationDOList, TrainStationQueryRespDTO.class);
    }

    @Override
    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture);
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        List<String> trainStationAllList = trainStationDOList.stream().map(TrainStationDO::getDeparture).collect(Collectors.toList());
        return List.of();
    }

    @Override
    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        return List.of();
    }
}
