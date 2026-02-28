package org.openzjl.index12306.biz.payservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置
 *
 * @author zhangjlk
 * @date 2026/2/28 上午10:41
 */
@Data
@Configuration
@ConfigurationProperties(prefix = AliPayProperties.PREFIX)
public class AliPayProperties {

    public static final String PREFIX = "pay.alipay";

    /**
     * 支付宝应用ID
     */
    private String appId;

    /**
     * 商户私钥
     */
    private String privateKey;

    /**
     * 支付宝公钥字符串
     */
    private String alipayPublicKey;

    /**
     * 网关地址
     */
    private String serverUrl;

    /**
     * 支付结果回调地址
     */
    private String notifyUrl;

    /**
     * 报文格式
     */
    private String format;

    /**
     * 字符串编码
     */
    private String charset;

    /**
     * 签名算法
     */
    private String signType;
}
