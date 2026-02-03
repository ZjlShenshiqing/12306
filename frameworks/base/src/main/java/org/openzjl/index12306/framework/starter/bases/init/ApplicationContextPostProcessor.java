/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.bases.init;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 有些场景是依赖 Spring 容器初始化完成后调用的，`ContextRefreshedEvent` 这个时间就比较合适。
 * 但是它除了初始化调用，容器刷新也会调用。

 * 为了避免容器刷新造成二次调用初始化逻辑，我们对一些比较常用的事件简单封装了一层逻辑。
 * @author zhangjlk
 * @date 2025/9/6 21:53
 */
@RequiredArgsConstructor
public class ApplicationContextPostProcessor implements ApplicationListener<ApplicationReadyEvent> {

    // Spring的容器
    private final ApplicationContext applicationContext;

    // 保证下面的逻辑只执行一次，防止重复触发
    private final AtomicBoolean executeOnlyOnce = new AtomicBoolean(false);


    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        /**
         * 当 Spring 启动完成后，onApplicationEvent 被调用。
         * 先判断是否已经执行过：
         * 第一次进来：compareAndSet(false, true) 成功 → 继续
         * 第二次进来：返回 false → 直接 return，不重复执行
         */
        if (!executeOnlyOnce.compareAndSet(false, true)) {
            return;
        }

        // 发布自己的事件：ApplicationInitializingEvent
        applicationContext.publishEvent(new ApplicationInitializingEvent(this));
    }
}
