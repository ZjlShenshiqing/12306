/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * 交易环境枚举（支付场景/交易类型）
 *
 * @author zhangjlk
 * @date 2026/1/23 15:55
 */
@RequiredArgsConstructor
public enum PayTradeTypeEnum {

    /**
     * 原生扫码支付（生成二维码，用户使用钱包扫码完成支付）
     */
    NATIVE(0),

    /**
     * JSAPI / 公众号内 / 小程序内支付
     */
    JSAPI(1),

    /**
     * H5 支付（浏览器中打开收银台）
     */
    MWEB(2),

    /**
     * DAPP 支付（在去中心化应用中唤起钱包支付）
     */
    DAPP(3)
    ;

    @Getter
    private final Integer code;

    /**
     * 根据 code 查找对应的交易类型名称（枚举名）
     *
     * @param code 交易类型编码
     * @return 枚举名，例如 NATIVE / JSAPI / MWEB / DAPP；不存在时返回 {@code null}
     */
    public static String findNameByCode(Integer code) {
        return Arrays.stream(PayTradeTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(PayTradeTypeEnum::name)
                .orElse(null);
    }
}
