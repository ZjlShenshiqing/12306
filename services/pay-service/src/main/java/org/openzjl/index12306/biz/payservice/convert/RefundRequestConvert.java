package org.openzjl.index12306.biz.payservice.convert;

import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.dto.base.RefundRequest;
import org.openzjl.index12306.biz.payservice.dto.command.RefundCommand;
import org.openzjl.index12306.biz.payservice.dto.req.AliRefundRequest;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;

import java.util.Objects;

/**
 * 退款请求入参转换器
 * <p>
 * 负责将通用的退款命令转换为具体支付渠道的退款请求对象，
 * 支持不同支付渠道的退款参数适配和转换。
 * 采用工具类设计模式，提供静态方法进行转换操作。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/1/28 12:39
 */
public final class RefundRequestConvert {

    /**
     * 将退款命令转换为退款请求对象
     * <p>
     * 根据支付渠道类型，将通用的 RefundCommand 转换为对应渠道的具体退款请求对象，
     * 确保不同支付渠道的退款参数能够正确适配到系统内部的处理流程。
     * </p>
     *
     * @param refundCommand 退款命令
     *                      <ul>
     *                        <li>包含支付渠道、订单信息、退款金额等通用字段</li>
     *                        <li>作为转换的数据源，提供基础退款信息</li>
     *                      </ul>
     * @return 退款请求对象
     *         <ul>
     *           <li>根据支付渠道类型返回对应的具体实现类</li>
     *           <li>目前支持：AliRefundRequest（支付宝退款请求）</li>
     *           <li>如果支付渠道不支持或参数无效，返回 null</li>
     *         </ul>
     * 
     * <p><strong>支持的支付渠道：</strong></p>
     * <ul>
     *   <li><strong>ALI_PAY：</strong>支付宝退款，转换为 AliRefundRequest</li>
     *   <li><strong>其他渠道：</strong>待扩展...</li>
     * </ul>
     * 
     * <p><strong>实现逻辑：</strong></p>
     * <ol>
     *   <li>初始化退款请求对象为 null</li>
     *   <li>判断支付渠道类型</li>
     *   <li>对于支付宝渠道：
     *     <ul>
     *       <li>使用 BeanUtil.convert 将命令对象转换为 AliRefundRequest</li>
     *     </ul>
     *   </li>
     *   <li>返回转换后的退款请求对象</li>
     * </ol>
     */
    public static RefundRequest command2RefundRequest(RefundCommand refundCommand) {
        // 初始化退款请求对象为 null
        RefundRequest refundRequest = null;
        
        // 判断支付渠道是否为支付宝
        if (Objects.equals(refundCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            // 将通用命令对象转换为支付宝特定的退款请求对象
            refundRequest = BeanUtil.convert(refundCommand, AliRefundRequest.class);
        }
        
        // 返回转换后的退款请求对象
        // 如果支付渠道不支持，返回 null
        return refundRequest;
    }
}
