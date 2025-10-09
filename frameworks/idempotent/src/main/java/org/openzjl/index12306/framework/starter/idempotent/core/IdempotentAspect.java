package org.openzjl.index12306.framework.starter.idempotent.core;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent;

import java.lang.reflect.Method;

/**
 * 幂等注解AOP拦截器
 *
 * @author zhangjlk
 * @date 2025/10/8 20:11
 */
@Aspect
public final class IdempotentAspect {

    /**
     * 使用幂等检查方法
     */
    @Around("@annotation(org.openzjl.index12306.framework.starter.idempotent.annotation.Idempotent)")
    public Object idempotentHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        // 拿到幂等注解
        Idempotent idempotent = getIdempotent(joinPoint);
        // 通过幂等注解拿到幂等处理器
        IdempotentExecuteHandler instance = IdempotentExecuteHandlerFactory.getInstance(idempotent.scene(), idempotent.type());
        Object resultObj;
        try {
            /**
             * instance.execute(...): 执行前置处理。这是幂等性检查的核心。
             * 这个处理器会根据具体的策略（比如 Token 策略），检查当前请求是否是重复提交。
             *
             * 如果检查通过 (不是重复请求)，方法正常执行完毕。
             *
             * 如果检查不通过 (是重复请求)，这个方法内部会直接抛出 RepeatConsumptionException 异常，后面的代码（joinPoint.proceed()）将不会被执行。
             */
            instance.execute(joinPoint, idempotent);

            /**
             * resultObj = joinPoint.proceed(): 执行真正的业务方法。只有在前置检查通过后，
             * 这行代码才会被调用，也就是执行你写的那个被 @Idempotent 注解的方法。
             * resultObj 是你业务方法的返回值。
             */
            resultObj = joinPoint.proceed();

            /**
             * instance.postProcessing(): 执行后置处理。如果业务方法成功执行完毕，会调用这个方法。
             * 它可能用来做一些清理工作，比如在某些模式下标记请求成功。
             */
            instance.postProcessing();
        } catch (RepeatConsumptionException ex) {
            // 重复请求不需要报错，而是静默处理
            if (!ex.getError()) {
                return null;
            }
            throw ex;
        } catch (Throwable ex) {
            instance.exceptionProcessing();
            throw ex;
        } finally {
            // 清理 ThreadLocal 上下文。这是为了防止内存泄漏，确保线程被归还到线程池时是干净的。
            IdempotentContext.clean();
        }
        return resultObj;
    }

    /**
     * 一个静态工具方法，用于从 AOP 的连接点（JoinPoint）中提取出 @Idempotent 注解。
     *
     * @param joinPoint ProceedingJoinPoint 提供了对连接点状态和静态信息的访问，比如获取方法签名、目标对象等。
     * @return 如果方法上存在 @Idempotent 注解，则返回该注解的实例对象；否则返回 null。
     * @throws NoSuchMethodException 如果在目标类中找不到具有指定名称和参数的方法。
     */
    public static Idempotent getIdempotent(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {

        // 1. 从连接点（joinPoint）获取方法的签名（Signature）。
        // 方法签名包含了被拦截方法的详细信息，如方法名、参数类型、返回类型等。
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        // 2. 使用 Java 反射（Reflection）来获取实际的 Method 对象。
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getParameterTypes());

        // 3. 从 Method 对象上获取 @Idempotent 注解。
        // .getAnnotation(...) 会检查该方法上是否存在指定的注解。
        // 如果 targetMethod 方法上确实有 @Idempotent 注解，这里就会返回该注解的实例。
        // 如果没有，则会返回 null。
        return targetMethod.getAnnotation(Idempotent.class);
    }
}
