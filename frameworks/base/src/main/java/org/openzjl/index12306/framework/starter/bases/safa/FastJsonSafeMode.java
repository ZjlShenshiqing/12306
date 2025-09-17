package org.openzjl.index12306.framework.starter.bases.safa;

import org.springframework.beans.factory.InitializingBean;

/**
 * FastJson安全模式，开启后关闭类型隐式传递
 * @author zhangjlk
 * @date 2025/9/6 21:52
 */
public class FastJsonSafeMode implements InitializingBean {

    /**
     * 在 Spring Bean 初始化完成后执行
     * 设置 FastJSON 2 的安全模式为开启状态
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 开启 FastJSON 2 安全模式
        System.setProperty("fastjson2.parser.safeMode", "true");
    }
}
