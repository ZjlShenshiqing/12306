/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.convention.exception;

import org.openzjl.index12306.framework.starter.convention.errorcode.BaseErrorCode;
import org.openzjl.index12306.framework.starter.convention.errorcode.IErrorCode;

import java.util.Optional;

/**
 * 服务端异常
 * @author zhangjlk
 * @date 2025/9/15 20:43
 */
public class ServiceException extends AbstractException{

    public ServiceException(String message) {
        this(message, null, BaseErrorCode.SERVICE_ERROR);
    }

    public ServiceException(IErrorCode errorCode) {
        this(null, errorCode);
    }

    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ServiceException(String message, Throwable cause, IErrorCode errorCode) {
        super(Optional.ofNullable(message).orElse(errorCode.message()), cause, errorCode);
    }

    @Override
    public String toString() {
        return "ServiceException{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
