package org.openzjl.index12306.biz.ticketservice.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.RouteDTO;
import org.openzjl.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.TrainStationService;
import org.openzjl.index12306.biz.ticketservice.toolkit.StationCalculateUtil;
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
        // 构建查询条件
        // ---------------------------------------------------------
        // Wrappers.lambdaQuery() 创建一个 LambdaQueryWrapper 对象，TrainStationDO 指定了要查询的数据库实体表
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                // 这一句翻译成 SQL 就是： WHERE train_id = trainId
                .eq(TrainStationDO::getTrainId, trainId);

        // ---------------------------------------------------------
        // 执行数据库查询
        // ---------------------------------------------------------
        // 调用 Mapper 接口的 selectList 方法，返回的结果是 DO (Data Object) 列表，即直接对应数据库表结构的实体对象
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        // ---------------------------------------------------------
        // 对象转换 (DO -> DTO)
        // ---------------------------------------------------------
        // BeanUtil.convert 是一个工具方法
        // 它会自动把 trainStationDOList 里每个对象的属性值，复制到新的 TrainStationQueryRespDTO 对象中。
        return BeanUtil.convert(trainStationDOList, TrainStationQueryRespDTO.class);
    }

    @Override
    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        // -----------------------------------------------------------------------
        // 构建数据库查询条件
        // -----------------------------------------------------------------------
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                // WHERE train_id = {trainId}：锁定具体的某一趟列车
                .eq(TrainStationDO::getTrainId, trainId)
                // 只查询出发站字段
                // 最后一段数据的样式：终点站 -> null
                .select(TrainStationDO::getDeparture);

        // -----------------------------------------------------------------------
        // 获取该列车的所有经停站信息
        // -----------------------------------------------------------------------
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        // -----------------------------------------------------------------------
        // 数据格式转换 (Entity -> List<String>)
        // -----------------------------------------------------------------------
        // 数据库查出来的是对象列表，通常只需要纯粹的站点名称列表
        List<String> trainStationAllList = trainStationDOList.stream()
                // 提取每个对象中的 'departure' 字段 (即站点名称)
                // 终点站
                .map(TrainStationDO::getDeparture)
                // 收集成一个字符串列表，例如：["北京南", "济南西", "南京南", "上海虹桥"]
                .collect(Collectors.toList());

        // 输入：整条线路的所有站点 (trainStationAllList)，用户买的起点 (departure)，用户买的终点 (arrival)
        // 输出：所有受影响的子区间列表
        //       例如用户买 "济南西" -> "南京南"，这里会返回 ["济南西->南京南", "济南西->上海虹桥", "北京南->南京南"...] 等所有冲突区间
        return StationCalculateUtil.throughStation(trainStationAllList, departure, arrival);
    }

    @Override
    public List<RouteDTO> listTakeoutTrainStationRoute(String trainId, String departure, String arrival) {
        // -----------------------------------------------------------------------
        // 准备查询条件
        // -----------------------------------------------------------------------
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                // 锁定当前操作的那一趟列车
                .eq(TrainStationDO::getTrainId, trainId)
                // 只查询出发站字段
                // 最后一段数据的样式：终点站 -> null
                .select(TrainStationDO::getDeparture);

        // -----------------------------------------------------------------------
        // 获取完整路线图
        // -----------------------------------------------------------------------
        // 拿到这趟车的所有经停站 (例如：北京南, 济南西, 南京南, 上海虹桥)
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);

        // -----------------------------------------------------------------------
        // 数据清洗 (Entity -> String)
        // -----------------------------------------------------------------------
        // 将数据库实体列表转换为纯粹的站点名称列表 List<String>
        List<String> trainStationAllList = trainStationDOList.stream()
                .map(TrainStationDO::getDeparture)
                .collect(Collectors.toList());

        // -----------------------------------------------------------------------
        // 调用核心扣减逻辑 (Critical!)
        // -----------------------------------------------------------------------
        // 调用 StationCalculateUtil.takeoutStation 方法。
        // 输入：完整站点列表, 用户买的起点 (departure), 用户买的终点 (arrival)
        // 输出：所有因为这张票卖出而需要扣减库存的区间集合。
        //       比如用户买 B->D，这里返回的就是 [A->C, A->D, A->E, B->C, B->D, B->E, C->D, C->E ...]
        return StationCalculateUtil.takeoutStation(trainStationAllList, departure, arrival);
    }
}
