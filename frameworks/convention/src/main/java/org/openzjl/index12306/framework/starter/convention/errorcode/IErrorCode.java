package org.openzjl.index12306.framework.starter.convention.errorcode;

/**
 * 平台错误码
 * @author zhangjlk
 * @date 2025/9/15 20:41
 */
public interface IErrorCode {

    /**
     * 错误码
     */
    String code();

    /**
     * 错误信息
     */
    String message();
}
