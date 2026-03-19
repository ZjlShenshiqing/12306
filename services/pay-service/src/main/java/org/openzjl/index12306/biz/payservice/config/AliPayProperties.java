/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.config;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayConfig;
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
     * 商户私钥 （暂时不使用）
     */
    private String privateKey;

    /**
     * 商户私钥（推荐字段）
     */
    private String merchantPrivateKey;

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

    public AlipayConfig toAlipayConfig() {
        AlipayConfig config = new AlipayConfig();
        config.setAppId(appId);
        config.setServerUrl(serverUrl);
        config.setAlipayPublicKey(alipayPublicKey);
        config.setPrivateKey(getEffectivePrivateKey());
        config.setFormat(format);
        config.setSignType(signType);
        config.setCharset(normalizeCharset(charset));
        return config;
    }

    public String getEffectivePrivateKey() {
        return StrUtil.isNotBlank(merchantPrivateKey) ? merchantPrivateKey : privateKey;
    }

    private String normalizeCharset(String sourceCharset) {
        if (StrUtil.isBlank(sourceCharset)) {
            return "UTF-8";
        }
        return "UTF8".equalsIgnoreCase(sourceCharset) ? "UTF-8" : sourceCharset;
    }
}
