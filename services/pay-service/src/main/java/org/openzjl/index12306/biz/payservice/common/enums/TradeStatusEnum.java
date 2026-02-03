/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.payservice.common.enums;

import cn.hutool.core.collection.ListUtil;
import org.openzjl.index12306.framework.starter.convention.exception.ServiceException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 交易状态枚举类
 * <p>
 * 定义支付交易的各种状态，用于统一管理和转换支付平台返回的交易状态。
 * 该枚举采用抽象方法的方式，为每个状态常量提供独立的状态码和状态值实现。
 * </p>
 *
 * @author zhangjlk
 * @date 2026/1/23 17:46
 */
public enum TradeStatusEnum {

    /**
     * 交易创建，等待买家付款
     * <p>
     * 状态说明：支付订单已创建，但买家尚未完成付款操作
     * 适用场景：用户发起支付请求后，跳转到支付页面但未完成支付
     * 状态码：0
     * </p>
     */
    WAIT_BUYER_PAY {
        /**
         * 获取当前交易状态的状态码
         * @return 状态码，固定为 0
         */
        @Override
        public Integer tradeCode() {
            return 0;
        }

        /**
         * 获取当前交易状态的字符串表示集合
         * @return 包含当前状态字符串的列表
         */
        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("WAIT_BUYER_PAY");
        }
    },

    /**
     * 交易关闭
     * <p>
     * 状态说明：交易未完成付款而关闭，或支付完成后进行了全额退款/部分退款
     * 适用场景：
     * <ul>
     *   <li>买家超时未付款，系统自动关闭交易</li>
     *   <li>买家主动取消交易</li>
     *   <li>交易完成后发生退款，导致交易状态变为关闭</li>
     * </ul>
     * 状态码：10
     * </p>
     */
    TRADE_CLOSED {
        /**
         * 获取当前交易状态的状态码
         * @return 状态码，固定为 10
         */
        @Override
        public Integer tradeCode() {
            return 10;
        }

        /**
         * 获取当前交易状态的字符串表示集合
         * @return 包含当前状态字符串的列表
         */
        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("TRADE_CLOSED");
        }
    },

    /**
     * 交易支付成功
     * <p>
     * 状态说明：买家已成功完成付款，交易处于成功状态
     * 适用场景：用户完成支付操作，支付平台返回支付成功
     * 状态码：20
     * </p>
     */
    TRADE_SUCCESS {
        /**
         * 获取当前交易状态的状态码
         * @return 状态码，固定为 20
         */
        @Override
        public Integer tradeCode() {
            return 20;
        }

        /**
         * 获取当前交易状态的字符串表示集合
         * @return 包含当前状态字符串的列表
         */
        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("TRADE_SUCCESS");
        }
    },

    /**
     * 交易结束，不可退款
     * <p>
     * 状态说明：交易已完成，且超过可退款期限，无法再进行退款操作
     * 适用场景：交易成功后，经过一定时间（如7天）自动进入此状态
     * 状态码：30
     * </p>
     */
    TRADE_FINISHED {
        /**
         * 获取当前交易状态的状态码
         * @return 状态码，固定为 30
         */
        @Override
        public Integer tradeCode() {
            return 30;
        }

        /**
         * 获取当前交易状态的字符串表示集合
         * @return 包含当前状态字符串的列表
         */
        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("TRADE_FINISHED");
        }
    };

    /**
     * 获取交易状态码
     * <p>
     * 每个交易状态常量都必须实现此方法，返回对应的状态码。
     * 状态码用于数据库存储和业务逻辑判断，具有唯一性。
     * </p>
     * @return 交易状态码，整数类型
     */
    public abstract Integer tradeCode();

    /**
     * 获取交易状态字符串集合
     * <p>
     * 每个交易状态常量都必须实现此方法，返回包含当前状态字符串表示的列表。
     * 此方法用于匹配支付平台返回的原始状态值。
     * </p>
     * @return 交易状态字符串列表
     */
    protected abstract List<String> tradeStatus();

    /**
     * 查询真实的交易状态
     * <p>
     * 根据支付平台返回的原始交易状态，查询并转换为系统内部统一的交易状态枚举值。
     * 用于将三方支付平台的状态标准化，确保系统内部状态的一致性。
     * </p>
     *
     * @param tradeStatus 三方支付平台返回的原始交易状态字符串
     * @return 系统内部统一的交易状态枚举名称，用于数据库存储
     * @throws ServiceException 如果未找到对应的交易状态，抛出服务异常
     */
    public static String queryActualTradeStatus(String tradeStatus) {
        // 遍历所有交易状态枚举，查找包含给定状态字符串的枚举常量
        Optional<TradeStatusEnum> tradeStatusEnum = Arrays.stream(TradeStatusEnum.values())
                .filter(each -> each.tradeStatus().contains(tradeStatus))
                .findFirst();
        
        // 如果找到对应的状态，返回枚举名称；否则抛出异常
        return tradeStatusEnum.orElseThrow(() -> new ServiceException("未找到支付状态")).toString();
    }

    /**
     * 查询真实的交易状态码
     * <p>
     * 根据支付平台返回的原始交易状态，查询并转换为系统内部统一的交易状态码。
     * 用于将三方支付平台的状态标准化为数字状态码，便于数据库存储和业务逻辑判断。
     * </p>
     *
     * @param tradeStatus 第三方支付平台返回的原始交易状态字符串
     * @return 系统内部统一的交易状态码，整数类型，用于数据库存储
     * @throws ServiceException 如果未找到对应的交易状态，抛出服务异常
     */
    public static Integer queryActualTradeStatusCode(String tradeStatus) {
        // 遍历所有交易状态枚举，查找包含给定状态字符串的枚举常量
        Optional<TradeStatusEnum> tradeStatusEnum = Arrays.stream(TradeStatusEnum.values())
                .filter(each -> each.tradeStatus().contains(tradeStatus))
                .findFirst();

        // 如果找到对应的状态，返回其状态码；否则抛出异常
        return tradeStatusEnum.orElseThrow(() -> new ServiceException("未找到支付状态")).tradeCode();
    }
}
