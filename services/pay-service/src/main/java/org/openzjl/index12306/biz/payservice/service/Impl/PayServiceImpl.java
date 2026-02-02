package org.openzjl.index12306.biz.payservice.service.Impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.payservice.common.enums.TradeStatusEnum;
import org.openzjl.index12306.biz.payservice.dao.entity.PayDO;
import org.openzjl.index12306.biz.payservice.dao.mapper.PayMapper;
import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;
import org.openzjl.index12306.biz.payservice.dto.base.PayResponse;
import org.openzjl.index12306.biz.payservice.dto.resp.PayInfoRespDTO;
import org.openzjl.index12306.biz.payservice.dto.resp.PayRespDTO;
import org.openzjl.index12306.biz.payservice.service.payid.PayIdGeneratorManager;
import org.openzjl.index12306.biz.payservice.service.PayService;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;
import org.openzjl.index12306.framework.starter.designpattern.staregy.AbstractStrategyChoose;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.openzjl.index12306.biz.payservice.common.constant.RedisKeyConstant.ORDER_PAY_RESULT_INFO;

/**
 * 支付接口服务实现类
 * <p>
 * 实现通用支付功能，支持多种支付方式的统一处理。
 * 采用策略模式选择具体的支付实现，使用分布式缓存提高性能，
 * 通过幂等性设计保证支付操作的安全性。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/1/23 16:23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    /**
     * 分布式缓存客户端
     * <p>
     * 用于缓存支付结果，提高重复请求的响应速度，减少数据库压力。
     * 缓存键格式：ORDER_PAY_RESULT_INFO + 订单号
     * 缓存过期时间：10分钟
     */
    private final DistributedCache distributedCache;
    
    /**
     * 策略选择器
     * <p>
     * 根据支付请求的标记选择对应的支付策略实现，
     * 支持不同支付方式的动态切换。
     */
    private final AbstractStrategyChoose abstractStrategyChoose;
    
    /**
     * 支付数据访问对象
     * <p>
     * 用于操作支付相关的数据库表，执行支付记录的插入操作。
     */
    private final PayMapper payMapper;

    /**
     * 通用支付方法
     * <p>
     * 处理各种支付方式的统一入口，支持幂等性操作，确保支付的安全性和一致性。
     * </p>
     *
     * @param requestParam 支付请求参数
     *                     <ul>
     *                       <li>包含订单号、支付方式、金额等支付相关信息</li>
     *                       <li>通过 buildMark() 方法生成支付策略选择标记</li>
     *                       <li>getOutOrderSn() 方法返回外部订单号，用于幂等性校验</li>
     *                     </ul>
     * @return 支付响应对象，包含支付结果和支付链接等信息
     * @throws ServiceException 如果支付单创建失败或支付处理过程中出现异常
     * 
     * <p><strong>幂等性保障：</strong></p>
     * <ul>
     *   <li>使用 @Idempotent 注解，基于外部订单号生成唯一锁键</li>
     *   <li>相同的外部订单号在短时间内重复请求会被视为同一操作</li>
     *   <li>防止用户重复点击导致的重复支付问题</li>
     * </ul>
     */
    @Idempotent(
            type = IdempotentTypeEnum.SPEL,
            uniqueKeyPrefix = "index12306-pay:lock_create_pay:",
            key = "#requestParam.getOutOrderSn()"
    )
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayRespDTO commonPay(PayRequest requestParam) {
        // 检查缓存中是否存在支付结果，存在则直接返回，提高性能
        PayRespDTO cacheResult = distributedCache.get(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(), PayRespDTO.class);
        if (cacheResult != null) {
            return cacheResult;
        }
        
        // 使用策略模式选择并执行对应的支付实现
        // requestParam.buildMark() 生成支付策略选择标记
        PayResponse result = abstractStrategyChoose.chooseAndExecuteResp(requestParam.buildMark(), requestParam);
        
        // 将支付请求参数转换为支付记录实体
        PayDO insertPay = BeanUtil.convert(requestParam, PayDO.class);
        
        // 生成唯一支付流水号
        // 格式：分布式ID前缀 + 订单号后6位
        String paySn = PayIdGeneratorManager.generateId(requestParam.getOrderSn());
        insertPay.setPaySn(paySn);
        
        // 设置初始交易状态为：等待买家付款
        insertPay.setStatus(TradeStatusEnum.WAIT_BUYER_PAY.tradeCode());
        
        // 金额处理：将 BigDecimal 类型的金额转换为分单位的整数
        // 1. 乘以 100 转换为分
        // 2. 四舍五入保留 0 位小数
        // 3. 转换为 int 类型存储
        insertPay.setTotalAmount(requestParam.getTotalAmount().multiply(new BigDecimal("100")).setScale(0, BigDecimal.ROUND_HALF_UP).intValue());
        
        // 执行数据库插入操作
        int insert = payMapper.insert(insertPay);
        
        // 验证插入结果，失败则记录错误日志并抛出异常
        if (insert <= 0) {
            log.error("支付单创建失败，支付聚合根：{}", JSON.toJSONString(requestParam));
            throw new ServiceException("支付单创建失败");
        }
        
        // 缓存支付结果，设置 10 分钟过期时间
        // 键格式：ORDER_PAY_RESULT_INFO + 订单号
        distributedCache.put(ORDER_PAY_RESULT_INFO + requestParam.getOrderSn(), JSON.toJSONString(result), 10, TimeUnit.MINUTES);
        
        // 将支付策略执行结果转换为响应对象并返回
        return BeanUtil.convert(result, PayRespDTO.class);
    }

    /**
     * 根据订单号查询支付信息
     * <p>
     * 通过订单号查询对应的支付记录信息，返回转换后的支付信息响应对象。
     * 用于查询特定订单的支付状态和详情。
     * </p>
     *
     * @param orderSn 订单号
     *                <ul>
     *                  <li>业务系统生成的唯一订单标识</li>
     *                  <li>与支付请求中的订单号对应</li>
     *                </ul>
     * @return 支付信息响应对象，包含支付状态、金额、支付时间等详细信息
     *         <ul>
     *           <li>如果找到对应的支付记录，返回转换后的响应对象</li>
     *           <li>如果未找到支付记录，返回 null</li>
     *         </ul>
     */
    @Override
    public PayInfoRespDTO getPayInfoByOrderSn(String orderSn) {
        // 构建查询条件：使用订单号作为精确匹配条件
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderSn, orderSn);
        // 执行数据库查询，获取唯一的支付记录
        PayDO payDO = payMapper.selectOne(queryWrapper);
        // 将支付记录实体转换为响应对象并返回
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }

    /**
     * 根据支付流水号查询支付信息
     * <p>
     * 通过支付流水号查询对应的支付记录信息，返回转换后的支付信息响应对象。
     * 用于查询特定支付交易的详细信息。
     * </p>
     *
     * @param paySn 支付流水号
     *              <ul>
     *                <li>系统生成的唯一支付交易标识</li>
     *                <li>格式：分布式ID前缀 + 订单号后6位</li>
     *                <li>由 PayIdGeneratorManager.generateId() 方法生成</li>
     *              </ul>
     * @return 支付信息响应对象，包含支付状态、金额、支付时间等详细信息
     *         <ul>
     *           <li>如果找到对应的支付记录，返回转换后的响应对象</li>
     *           <li>如果未找到支付记录，返回 null</li>
     *         </ul>
     */
    @Override
    public PayInfoRespDTO getPayInfoByPaySn(String paySn) {
        // 构建查询条件：使用支付流水号作为精确匹配条件
        LambdaQueryWrapper<PayDO> queryWrapper = Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getPaySn, paySn);
        // 执行数据库查询，获取唯一的支付记录
        PayDO payDO = payMapper.selectOne(queryWrapper);
        // 将支付记录实体转换为响应对象并返回
        return BeanUtil.convert(payDO, PayInfoRespDTO.class);
    }
}
