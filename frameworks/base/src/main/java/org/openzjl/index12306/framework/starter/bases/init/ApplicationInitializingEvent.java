package org.openzjl.index12306.framework.starter.bases.init;

import org.springframework.context.ApplicationEvent;

/**
 * 应用初始化事件
 *
 * 它的价值在于它的名字和身份。
 * 它是一个“规约事件”，即团队内部的**一个约定**。
 * 约定内容
 * “项目中所有需要在启动时、且仅执行一次的业务逻辑，
 * 都应该监听这个 `ApplicationInitializingEvent` 信号，而不是 Spring 原生的信号。”
 * @author zhangjlk
 * @date 2025/9/6 21:52
 */
public class ApplicationInitializingEvent extends ApplicationEvent {

    /**
     * 它本身几乎没有代码，像一个空的信封。
     *
     * 它的价值在于它的名字和身份。它是一个“规约事件”，即团队内部的一个约定。
     *
     * 约定内容：“项目中所有需要在启动时、且仅执行一次的业务逻辑，都应该监听这个 ApplicationInitializingEvent 信号，而不是 Spring 原生的信号。
     * @param source
     */
    public ApplicationInitializingEvent(Object source) {
        super(source);
    }
}
