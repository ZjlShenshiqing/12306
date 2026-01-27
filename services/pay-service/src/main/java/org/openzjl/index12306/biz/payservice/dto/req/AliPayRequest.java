package org.openzjl.index12306.biz.payservice.dto.req;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.openzjl.index12306.biz.payservice.common.enums.PayChannelEnum;
import org.openzjl.index12306.biz.payservice.common.enums.PayTradeTypeEnum;
import org.openzjl.index12306.biz.payservice.dto.base.AbstractPayRequest;
import org.openzjl.index12306.biz.payservice.dto.base.PayRequest;

import java.math.BigDecimal;

/**
 * 支付宝支付请求入参
 * <p>
 * 封装调用支付宝支付接口所需的所有参数，继承自 {@link AbstractPayRequest}，包含：
 * <ul>
 *   <li>基础信息：交易类型（{@link PayTradeTypeEnum}）、订单号、支付渠道等（来自父类）</li>
 *   <li>支付宝特有字段：商户订单号、订单金额、订单标题、交易凭证号等</li>
 * </ul>
 * <p>
 *
 * @author zhangjlk
 * @date 2026/1/22 12:42
 * @see AbstractPayRequest
 * @see PayRequest
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public final class AliPayRequest extends AbstractPayRequest {

    /**
     * 商户订单号（支付宝侧）
     * <p>
     * 商户系统内部订单号，用于标识一笔订单，需保证在商户系统内唯一。
     * <p>
     * 格式要求：
     * <ul>
     *   <li>长度：1-64 个字符</li>
     *   <li>仅支持数字、字母、下划线、中划线</li>
     *   <li>建议使用时间戳 + 随机数组合，确保唯一性</li>
     * </ul>
     * <p>
     * 示例：{@code "202601231234567890"} 或 {@code "ORDER_20260123_001"}
     */
    private String outOrderSn;

    /**
     * 订单总金额
     * <p>
     * 订单需要支付的金额，单位为元（人民币）。
     * <p>
     * 格式要求：
     * <ul>
     *   <li>精确到小数点后两位（如：100.00）</li>
     *   <li>最小金额：0.01 元</li>
     *   <li>最大金额：根据支付宝限制（通常为 50000.00 元）</li>
     * </ul>
     * <p>
     * 示例：{@code new BigDecimal("100.00")} 表示 100 元整
     */
    private BigDecimal totalAmount;

    /**
     * 订单标题
     * <p>
     * 订单的简要描述，会显示在支付宝收银台和账单中，用于用户识别订单内容。
     * <p>
     * 格式要求：
     * <ul>
     *   <li>长度：1-256 个字符</li>
     *   <li>建议简洁明了，突出订单核心信息</li>
     *   <li>避免包含特殊字符和敏感信息</li>
     * </ul>
     * <p>
     * 示例：{@code "12306火车票订单"}、{@code "北京-上海 G123次 一等座"}
     */
    private String subject;

    /**
     * 交易凭证号
     * <p>
     * 支付宝返回的交易凭证号（交易号），用于标识支付宝侧的一笔交易。
     * 通常在支付成功后由支付宝回调或查询接口返回，可用于后续的订单查询、退款等操作。
     * <p>
     * 格式说明：
     * <ul>
     *   <li>由支付宝系统生成，格式为：{@code 2026012322001234567890123456}</li>
     *   <li>长度为 28 位数字</li>
     *   <li>在创建支付请求时通常为空，支付成功后由支付宝返回</li>
     * </ul>
     * <p>
     * 注意：此字段主要用于存储支付宝返回的交易号，创建支付请求时可不填。
     */
    private String tradeNo;

    /**
     * 获取当前支付宝支付请求对象
     * <p>
     * 实现 {@link PayRequest#getAliPayRequest()} 接口方法，用于策略模式中识别支付渠道类型。
     * 当支付请求为支付宝类型时，返回当前对象；其他渠道返回 {@code null}。
     *
     * @return 当前 {@link AliPayRequest} 实例，即 {@code this}
     */
    @Override
    public AliPayRequest getAliPayRequest() {
        return this;
    }

    /**
     * 构建支付策略标识（Mark）
     * <p>
     * 用于策略模式中定位具体的支付策略实现类。标识格式为：
     * <ul>
     *   <li>基础格式：{@code "支付宝"}</li>
     *   <li>带交易类型：{@code "ALI_PAY_NATIVE"}、{@code "ALI_PAY_JSAPI"} 等</li>
     * </ul>
     * <p>
     * 构建逻辑：
     * <ol>
     *   <li>如果 {@link #getTradeType()} 为空，返回基础渠道名称：{@code "支付宝"}</li>
     *   <li>如果存在交易类型，返回：{@code "ALI_PAY_" + 交易类型枚举名}</li>
     * </ol>
     * <p>
     * 示例返回值：
     * <ul>
     *   <li>{@code "支付宝"} - 当交易类型为空时</li>
     *   <li>{@code "ALI_PAY_NATIVE"} - 扫码支付</li>
     *   <li>{@code "ALI_PAY_JSAPI"} - 公众号/小程序支付</li>
     *   <li>{@code "ALI_PAY_MWEB"} - H5 支付</li>
     * </ul>
     *
     * @return 支付策略标识字符串，用于匹配对应的支付策略实现类
     * @see PayChannelEnum
     * @see PayTradeTypeEnum
     */
    @Override
    public String buildMark() {
        String mark = PayChannelEnum.ALI_PAY.getName();
        if (getTradeType() != null) {
            mark = PayChannelEnum.ALI_PAY.name() + "_" + PayTradeTypeEnum.findNameByCode(getTradeType());
        }
        return mark;
    }
}
