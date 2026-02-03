/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.idempotent.toolkit;

import cn.hutool.core.util.ArrayUtil;
import com.google.common.collect.Lists;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;

/**
 * SpEL 表达式解析工具
 *
 * @author zhangjlk
 * @date 2025/10/5 18:59
 */
public class SpELUtil {

    /**
     * 校验并返回实际使用的 SpEL 表达式
     *
     * 规则：
     * - 如果表达式包含 # 或 T(，说明是 SpEL 表达式 → 解析它
     * - 否则，直接返回原字符串（当作普通字符串处理）
     *
     * @param spEl SpEL 表达式（如 "#user.id"、"T(java.lang.Math).random()"）
     * @param method 当前执行的方法对象（用于获取参数名）
     * @param contextObj 方法参数值数组（用于绑定到 SpEL 上下文）
     * @return 解析后的结果（可能是 String、Long、Object 等）
     */
    public static Object parseKey(String spEl, Method method, Object[] contextObj) {
        // 定义 SpEL 的标志性符号：# 表示参数引用，T( 表示调用静态方法
        ArrayList<String> spELFlag = Lists.newArrayList("#", "T(");

        // 检查表达式是否包含任一 SpEL 标志
        Optional<String> optional = spELFlag.stream()
                .filter(spEl::contains)
                .findFirst();

        if (optional.isPresent()) {
            // 是 SpEL 表达式 → 调用 parse 方法解析
            return parse(spEl, method, contextObj);
        }

        // 不是 SpEL 表达式 → 直接返回原字符串（比如固定值 "order:123"）
        return spEl;
    }

    /**
     * 实际解析 SpEL 表达式的核心方法
     *
     * 步骤：
     * 1. 创建 SpEL 解析器
     * 2. 获取方法参数名（如 user、orderId）
     * 3. 构建上下文环境，绑定参数值
     * 4. 执行表达式，返回结果
     *
     * @param spEl SpEL 表达式
     * @param method 方法对象
     * @param contextObj 方法参数值数组
     * @return 解析结果
     */
    public static Object parse(String spEl, Method method, Object[] contextObj) {
        // 1. 创建参数名发现器（用于获取方法参数名，如 user、orderId）
        DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

        // 2. 创建 SpEL 解析器
        ExpressionParser parser = new SpelExpressionParser();

        // 3. 解析表达式
        Expression expression = parser.parseExpression(spEl);

        // 4. 获取方法参数名数组（如 ["user", "orderId"]）
        String[] params = discoverer.getParameterNames(method);

        // 5. 创建标准评估上下文（EvaluationContext）
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 6. 如果有参数名，将参数值绑定到上下文中（如 user -> User 对象，orderId -> 123）
        if (ArrayUtil.isNotEmpty(params)) {
            for (int len = 0; len < params.length; len++) {
                context.setVariable(params[len], contextObj[len]);
            }
        }

        // 7. 执行表达式，返回结果（如 "user123"、123、true 等）
        return expression.getValue(context);
    }
}
