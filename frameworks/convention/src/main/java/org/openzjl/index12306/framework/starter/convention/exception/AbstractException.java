package org.openzjl.index12306.framework.starter.convention.exception;

import lombok.Getter;
import org.openzjl.index12306.framework.starter.convention.errorcode.IErrorCode;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 抽象项目中的三类异常体系：客户端异常，服务端异常和远程调用服务异常
 * @author zhangjlk
 * @date 2025/9/15 20:42
 */
@Getter
public abstract class AbstractException extends RuntimeException {

    /**
     * 错误码
     */
    public final String errorCode;

    /**
     * 错误信息
     */
    public final String errorMessage;

    public AbstractException(String message, Throwable cause, IErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode.code();
        this.errorMessage = Optional.ofNullable(StringUtils.hasLength(message) ? message : null).orElse(errorCode.message());
    }
}
