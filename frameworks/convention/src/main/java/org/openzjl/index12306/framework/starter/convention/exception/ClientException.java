package org.openzjl.index12306.framework.starter.convention.exception;

import org.openzjl.index12306.framework.starter.convention.errorcode.BaseErrorCode;
import org.openzjl.index12306.framework.starter.convention.errorcode.IErrorCode;

/**
 * 客户端异常
 * @author zhangjlk
 * @date 2025/9/15 20:43
 */
public class ClientException extends AbstractException{

    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.CLIENT_ERROR);
    }

    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ClientException(String message, Throwable cause, IErrorCode errorCode) {
        super(message, cause, errorCode);
    }

    @Override
    public String toString() {
        return "ClientException{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
