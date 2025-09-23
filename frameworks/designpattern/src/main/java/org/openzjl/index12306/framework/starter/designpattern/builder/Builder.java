package org.openzjl.index12306.framework.starter.designpattern.builder;

import java.io.Serializable;

/**
 * Builder模式抽象接口
 * @author zhangjlk
 * @date 2025/9/17 19:42
 */
public interface Builder<T> extends Serializable {

    /**
     * 构建方法
     *
     * @return 构建后的对象
     */
    T build();
}
