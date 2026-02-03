/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import org.openzjl.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import org.openzjl.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import org.openzjl.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import org.openzjl.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import org.openzjl.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import org.openzjl.index12306.biz.ticketservice.remote.UserRemoteService;
import org.openzjl.index12306.biz.ticketservice.remote.dto.PassengerRespDTO;
import org.openzjl.index12306.biz.ticketservice.service.SeatService;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import org.openzjl.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import org.openzjl.index12306.framework.starter.convention.exception.RemoteException;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractStrategyChoose;
import org.openzjl.index12306.framework.starter.user.core.UserContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 购票时列车座位选择器
 *
 * @author zhangjlk
 * @date 2025/12/30 上午10:16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainSeatTypeSelector {

    private final SeatService seatService;
    private final UserRemoteService userRemoteService;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private TrainStationPriceMapper trainStationPriceMapper;
    private final ThreadPoolExecutor selectSeatThreadPoolExecutor;

    /**
     * 选座主方法
     * <p>
     * 功能说明：
     * 根据车次类型和购票请求，为所有乘客分配座位。支持多种座位类型的并发选座，
     * 提高选座效率。如果只有一种座位类型，则串行处理，避免不必要的线程开销。
     * <p>
     * 处理流程：
     * 1. 按座位类型分组：将乘客按选择的座位类型分组（如：商务座、一等座、二等座）
     * 2. 并发/串行选座：根据座位类型数量决定使用并发还是串行处理
     * 3. 结果验证：验证选座结果是否完整（每个乘客都分配到了座位）
     * 4. 提取乘客ID：从选座结果中提取乘客ID，用于后续处理
     *
     * @param trainType 车次类型编码（如：0=高速铁路，1=动车等）
     * @param requestParam 购票请求参数（包含车次ID、出发站、到达站、乘客列表等）
     * @return 选座结果列表，包含每个乘客分配到的座位信息
     * @throws ServiceException 如果选座失败（如余票不足），抛出业务异常
     */
    public List<TrainPurchaseTicketRespDTO> select(Integer trainType, PurchaseTicketReqDTO requestParam) {
        // 按座位类型分组
        // 获取所有乘客详情列表
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        
        // 按座位类型分组，将乘客按选择的座位类型分类
        // 例如：如果有5个乘客，3个选择商务座，2个选择一等座
        // 结果：{0: [乘客1, 乘客2, 乘客3], 1: [乘客4, 乘客5]}
        // Key: 座位类型编码（0=商务座，1=一等座，2=二等座）
        // Value: 选择该座位类型的乘客列表
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        
        // 创建线程安全的结果列表，用于存储所有选座结果
        // 使用Collections.synchronizedList确保多线程并发写入时的线程安全
        // 初始容量设置为seatTypeMap.size()，减少扩容次数
        List<TrainPurchaseTicketRespDTO> actualResult = Collections.synchronizedList(new ArrayList<>(seatTypeMap.size()));
        
        // 并发或串行选座
        // 判断是否有多种座位类型
        if (seatTypeMap.size() > 1) {
            // 多种座位类型：使用线程池并发处理，提高效率
            // 例如：同时为商务座、一等座、二等座的乘客选座
            
            // 创建Future列表，用于收集异步任务的Future对象
            List<Future<List<TrainPurchaseTicketRespDTO>>> futureResults = new ArrayList<>(seatTypeMap.size());
            
            // 为每种座位类型提交异步选座任务到线程池
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 提交选座任务到线程池，立即返回Future对象（不阻塞）
                // 任务会在后台线程中执行，多个座位类型的选座任务并发执行
                Future<List<TrainPurchaseTicketRespDTO>> completableFuture = selectSeatThreadPoolExecutor
                        .submit(() -> distributeSeats(trainType, seatType, requestParam, passengerSeatDetails));
                // 将Future对象添加到列表中，用于后续收集结果
                futureResults.add(completableFuture);
            });
            
            // 等待所有异步任务完成，并收集结果
            // 使用parallelStream()并行处理Future列表，提高效率
            futureResults.parallelStream().forEach(completableFuture -> {
                try {
                    // future.get() 会阻塞等待任务完成，然后获取选座结果
                    // 将每个座位类型的选座结果合并到总结果列表中
                    actualResult.addAll(completableFuture.get());
                } catch (Exception e) {
                    // 如果选座失败（如余票不足），抛出业务异常
                    // 注意：这里捕获的是Exception，包括ExecutionException（任务执行异常）
                    throw new ServiceException("站点余票不足");
                }
            });
        } else {
            // 只有一种座位类型：串行处理，避免不必要的线程开销
            // 例如：所有乘客都选择商务座，直接串行处理即可
            seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
                // 直接调用选座方法，同步执行
                List<TrainPurchaseTicketRespDTO> aggregationResult = distributeSeats(trainType, seatType, requestParam, passengerSeatDetails);
                // 将选座结果添加到总结果列表中
                actualResult.addAll(aggregationResult);
            });
        }
        
        // 验证选座结果
        // 检查选座结果是否完整
        // 条件1：actualResult为空，说明没有选到任何座位
        // 条件2：actualResult.size() != passengerDetails.size()，说明部分乘客没有分配到座位
        // 如果任一条件满足，说明选座失败，抛出异常
        if (CollUtil.isEmpty(actualResult) || !Objects.equals(actualResult.size(), passengerDetails.size())) {
            throw new ServiceException("站点余票不足，请尝试更换座位或选择其他站点");
        }
        
        // 提取乘客ID并验证乘客信息
        // 从选座结果中提取所有乘客ID，用于后续处理（如验证乘客信息、创建订单等）
        // 例如：从选座结果中提取 ["passenger1", "passenger2", "passenger3"]
        List<String> passengerIds = actualResult.stream()
                .map(TrainPurchaseTicketRespDTO::getPassengerId)
                .collect(Collectors.toList());
        
        // 调用远程用户服务，根据乘客ID批量查询乘客详细信息
        // 目的：验证乘客信息是否存在、是否有效，确保购票的乘客信息正确
        // 例如：验证乘客身份证号、姓名、联系方式等信息是否匹配
        Result<List<PassengerRespDTO>> passengerRemoteResult;
        List<PassengerRespDTO> passengerRemoteResultList;
        try {
            // 调用远程用户服务，批量查询乘客信息
            // 参数1：当前登录用户名，用于权限验证
            // 参数2：乘客ID列表，需要查询的乘客ID集合
            // 返回：Result包装的乘客信息列表
            passengerRemoteResult = userRemoteService.listPassengerQueryByIds(UserContext.getUserName(), passengerIds);
            
            // 验证远程调用结果
            // 条件1：!passengerRemoteResult.isSuccess() - 远程调用失败（如网络异常、服务异常等）
            // 条件2：CollUtil.isEmpty(passengerRemoteResultList = passengerRemoteResult.getData()) - 返回数据为空
            if (!passengerRemoteResult.isSuccess() || CollUtil.isEmpty(passengerRemoteResultList = passengerRemoteResult.getData())) {
                throw new RemoteException("用户远程调用查询乘车人相关信息错误！");
            }
        } catch (Throwable ex) {
            // 捕获所有异常（包括RemoteException和其他异常）
            if (ex instanceof RemoteException) {
                // 如果是远程调用异常（业务异常），只记录错误信息，不记录堆栈
                // 因为这是预期的业务异常，堆栈信息可能不必要
                log.error("用户服务远程调用查询乘车人相关信息错误，当前用户：{}，请求参数：{}", UserContext.getUserName(), passengerIds);
            } else {
                // 如果是其他异常（如网络异常、系统异常等），记录完整的错误信息和堆栈
                // 这些异常是未预期的，需要完整的堆栈信息用于排查问题
                log.error("用户服务远程调用查询乘车人相关信息错误，当前用户：{}，请求参数：{}", UserContext.getUserName(), passengerIds, ex);
            }
            // 重新抛出异常，终止选座流程
            // 因为乘客信息验证失败，无法继续后续的购票流程
            throw ex;
        }
        
        // 补充选座结果的详细信息
        // 为每个选座结果补充乘客的详细信息（身份证号、手机号、姓名等）和座位价格
        // 这些信息在选座时可能没有完整获取，需要从远程服务查询结果和数据库中补充
        actualResult.forEach(each -> {
            // 获取当前选座结果对应的乘客ID
            String passengerId = each.getPassengerId();
            
            // 从远程服务查询结果中查找对应的乘客信息
            // 通过乘客ID匹配，找到该乘客的完整信息（身份证号、手机号、姓名等）
            passengerRemoteResultList.stream()
                    // 过滤出ID匹配的乘客信息
                    .filter(item -> Objects.equals(item.getId(), passengerId))
                    // 获取第一个匹配的乘客信息（理论上应该只有一个）
                    .findFirst()
                    // 如果找到了乘客信息，则补充到选座结果中
                    .ifPresent(passenger -> {
                        // 补充身份证号：用于后续创建订单和实名制验证
                        each.setIdCard(passenger.getIdCard());
                        // 补充手机号：用于订单通知和联系
                        each.setPhone(passenger.getPhone());
                        // 补充证件类型：用于区分身份证、护照等不同证件类型
                        each.setUserType(passenger.getIdType());
                        // 补充真实姓名：用于订单显示和实名制验证
                        each.setRealName(passenger.getRealName());
                    });

            // 查询座位价格
            // 从数据库查询该座位的价格信息
            // 查询条件：车次ID、出发站、到达站、座位类型
            LambdaQueryWrapper<TrainStationPriceDO> lambdaQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    // 条件1：车次ID必须匹配
                    .eq(TrainStationPriceDO::getTrainId, requestParam.getTrainId())
                    // 条件2：出发站必须匹配
                    .eq(TrainStationPriceDO::getDeparture, requestParam.getDeparture())
                    // 条件3：到达站必须匹配
                    .eq(TrainStationPriceDO::getArrival, requestParam.getArrival())
                    // 条件4：座位类型必须匹配（如：商务座、一等座、二等座）
                    .eq(TrainStationPriceDO::getSeatType, each.getSeatType())
                    // 只查询价格字段，提高查询效率
                    .select(TrainStationPriceDO::getPrice);
            
            // 执行查询，获取该座位的价格信息
            // 注意：这里假设一定能查询到价格，如果查询不到可能会抛出异常
            TrainStationPriceDO trainStationPriceDO = trainStationPriceMapper.selectOne(lambdaQueryWrapper);
            
            // 将价格设置到选座结果中
            // 价格单位：分（数据库存储格式），后续可能需要转换为元
            each.setAmount(trainStationPriceDO.getPrice());
        });
        
        // 锁定座位
        // 锁定所有已选中的座位，防止其他用户同时选择相同的座位
        seatService.lockSeat(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival(), actualResult);
        
        // 返回完整的选座结果列表
        // 包含：座位信息、乘客信息、价格信息等，用于后续创建订单
        return actualResult;
    }


    /**
     * 分配座位（使用策略模式）
     * <p>
     * 功能说明：
     * 根据车次类型和座位类型，使用策略模式选择对应的选座策略并执行选座操作。
     * 不同的车次类型（如高铁、动车、普通列车）和座位类型（如商务座、一等座、二等座）
     * 组合起来会有不同的选座算法，通过策略模式实现灵活扩展。
     * <p>
     * 策略模式说明：
     * - 策略Key格式：车次类型名称 + 座位类型名称
     * - 例如：HIGH_SPEED_RAILWAY + BUSINESS_CLASS = "HIGH_SPEED_RAILWAYBUSINESS_CLASS"
     * - 系统会根据这个Key找到对应的选座策略实现类并执行
     * <p>
     * 使用场景：
     * - 高铁商务座：可能有特殊的选座规则（如优先选择靠窗位置）
     * - 动车二等座：可能有不同的选座算法（如优先选择连座）
     * - 普通列车硬座：可能有更简单的选座逻辑
     * <p>
     *
     * @param trainType 车次类型编码（如：0=高速铁路，1=动车等）
     * @param seatType 座位类型编码（如：0=商务座，1=一等座，2=二等座等）
     * @param requestParam 购票请求参数（包含车次ID、出发站、到达站、出发日期等）
     * @param passengerSeatDetails 该座位类型对应的乘客详情列表（已按座位类型分组）
     * @return 选座结果列表，包含每个乘客分配到的座位信息
     * @throws ServiceException 如果选座失败（如余票不足、座位已被占用等），抛出业务异常
     */
    private List<TrainPurchaseTicketRespDTO> distributeSeats(Integer trainType, Integer seatType, PurchaseTicketReqDTO requestParam, List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails) {
        // 构建策略Key：车型名称 + 座位类型名称
        // 例如：HIGH_SPEED_RAILWAY + BUSINESS_CLASS = "HIGH_SPEED_RAILWAYBUSINESS_CLASS"
        // 这个Key用于在策略选择器中找到对应的选座策略实现类
        String buildStrategyKey = VehicleTypeEnum.findNameByCode(trainType) + VehicleSeatTypeEnum.findNameByCode(seatType);
        
        // 构建选座DTO对象，封装选座所需的所有信息
        SelectSeatDTO selectSeatDTO = SelectSeatDTO.builder()
                // 座位类型编码（如：0=商务座，1=一等座，2=二等座）
                .seatType(seatType)
                // 该座位类型对应的乘客详情列表
                // 例如：如果seatType=0（商务座），这里就是所有选择商务座的乘客
                .passengerSeatDetails(passengerSeatDetails)
                // 购票请求参数（包含车次ID、出发站、到达站、出发日期等完整信息）
                .requestParam(requestParam)
                .build();
        
        try {
            // 通过策略选择器找到对应的选座策略并执行
            // chooseAndExecuteResp() 方法会根据 buildStrategyKey 找到对应的策略实现类
            // 然后调用策略的选座方法，返回选座结果列表
            // 例如：如果 buildStrategyKey = "HIGH_SPEED_RAILWAYBUSINESS_CLASS"
            // 就会找到 HighSpeedRailwayBusinessClassSeatStrategy 并执行其选座逻辑
            return abstractStrategyChoose.chooseAndExecuteResp(buildStrategyKey, selectSeatDTO);
        } catch (ServiceException e) {
            // 捕获业务异常并重新抛出
            // 选座失败的原因可能是：
            // 1. 余票不足：该座位类型没有足够的余票
            // 2. 座位已被占用：并发场景下座位被其他用户占用
            // 3. 选座策略执行失败：策略内部逻辑出错
            throw new ServiceException(e.getMessage());
        }
    }
}
