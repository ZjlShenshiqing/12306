/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.convention.exception;

import org.openzjl.index12306.framework.starter.convention.errorcode.BaseErrorCode;
import org.openzjl.index12306.framework.starter.convention.errorcode.IErrorCode;

/**
 * 远程服务调用异常
 * @author zhangjlk
 * @date 2025/9/15 20:43
 */
public class RemoteException extends AbstractException {

    public RemoteException(String message) {
        this(message, null, BaseErrorCode.REMOTE_ERROR);
    }

    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public RemoteException(String message, Throwable cause, IErrorCode errorCode) {
        super(message, cause, errorCode);
    }

    @Override
    public String toString() {
        return "RemoteException{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
