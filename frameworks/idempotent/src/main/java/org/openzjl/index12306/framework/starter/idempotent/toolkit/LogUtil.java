package org.openzjl.index12306.framework.starter.idempotent.toolkit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类：根据切面（AOP）上下文动态获取当前方法所在类的日志对象
 *
 * 使用场景：
 * - 在 AOP 切面中（如 @Around、@Before），需要记录日志时，动态获取目标类的 Logger
 * - 避免硬编码 Logger 类名，提升可维护性
 *
 * @author zhangjlk
 * @date 2025/10/5 18:59
 */
public class LogUtil {

    /**
     * 根据 AOP 切面上下文，获取当前被拦截方法所属类的日志对象
     *
     * @param joinPoint AOP 切点对象，包含方法签名、参数、目标对象等信息
     * @return 当前方法所在类的 Logger 实例
     */
    public static Logger getLog(ProceedingJoinPoint joinPoint) {
        // 1. 获取方法签名（MethodSignature）
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        // 2. 获取该方法所在的类（即目标类）
        Class<?> declaringClass = methodSignature.getDeclaringType();

        // 3. 使用 SLF4J 的 LoggerFactory 创建对应类的日志对象
        return LoggerFactory.getLogger(declaringClass);
    }
}
