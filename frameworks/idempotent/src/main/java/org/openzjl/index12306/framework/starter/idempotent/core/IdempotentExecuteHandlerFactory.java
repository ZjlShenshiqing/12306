package org.openzjl.index12306.framework.starter.idempotent.core;

import org.openzjl.index12306.framework.starter.bases.ApplicationContextHolder;
import org.openzjl.index12306.framework.starter.idempotent.core.param.IdempotentParamService;
import org.openzjl.index12306.framework.starter.idempotent.core.spel.IdempotentSpELByMQExecuteHandler;
import org.openzjl.index12306.framework.starter.idempotent.core.spel.IdempotentSpELService;
import org.openzjl.index12306.framework.starter.idempotent.core.token.IdempotentTokenService;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import org.openzjl.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;

/**
 * 幂等执行处理器工厂
 * <p>
 * @author zhangjlk
 * @date 2025/10/8 20:34
 */
public final class IdempotentExecuteHandlerFactory {

    /**
     * 获取指定的幂等处理器。
     * 这是一个静态工厂方法，外部调用者无需创建工厂实例，直接通过类名调用即可。
     *
     * @param scene 指定幂等验证场景类型 (例如 REST API 调用、MQ 消息消费)
     * @param type  指定幂等处理类型 (例如 基于参数验证、基于 Token 验证)
     * @return      返回一个具体的幂等执行处理器实例
     */
    public static IdempotentExecuteHandler getInstance(IdempotentSceneEnum scene, IdempotentTypeEnum type) {
        // 初始化结果变量为 null
        IdempotentExecuteHandler result = null;

        // 外层 switch：根据“场景”来做第一次决策
        switch (scene) {
            // 场景一：REST API 接口
            case RESTAPI -> {
                // 内层 switch：在 REST API 场景下，再根据“类型”来做第二次决策
                switch (type) {
                    // 类型1：基于请求参数进行幂等验证
                    // 从 Spring 容器中获取已经注册好的 IdempotentParamService 这个 Bean
                    case PARAM -> result = ApplicationContextHolder.getBean(IdempotentParamService.class);
                    // 类型2：基于 Token 进行幂等验证
                    case TOKEN -> result = ApplicationContextHolder.getBean(IdempotentTokenService.class);
                    // 类型3：基于 SpEL 表达式进行幂等验证
                    case SPEL -> result = ApplicationContextHolder.getBean(IdempotentSpELService.class);
                    // 如果是其它不支持的类型，则什么都不做，result 保持为 null
                    default -> {
                    }
                }
            }
            // 场景二：MQ 消息消费
            case MQ -> result = ApplicationContextHolder.getBean(IdempotentSpELByMQExecuteHandler.class);
            // 如果是其它不支持的场景，也什么都不做
            default -> {
            }
        }
        // 返回最终决策出的处理器实例
        return result;
    }
}
