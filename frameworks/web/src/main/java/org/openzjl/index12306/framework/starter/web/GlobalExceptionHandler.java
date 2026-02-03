/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.framework.starter.convention.errorcode.BaseErrorCode;
import org.openzjl.index12306.framework.starter.convention.exception.AbstractException;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * 全局异常处理器
 *
 * @author zhangjlk
 * @date 2025/10/4 14:44
 */
@Slf4j
@RestControllerAdvice // @RestControllerAdvice：捕获异常，并返回统一的格式
public final class GlobalExceptionHandler {

    /**
     * 拦截参数验证异常
     */
    @SneakyThrows // 它自动帮你把“受检异常”包装成“非受检异常（RuntimeException）”，让你不用写 throws 或 try-catch，代码更简洁。
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
        // 1. 获取参数校验结果对象 —— 它保存了所有校验失败的字段和错误消息
        BindingResult bindingResult = ex.getBindingResult();
        // 2. 从所有错误中取出第一个错误字段的信息（Hutool 工具类，安全取第一个，避免空指针）
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        // 3. 如果有错误，取错误消息；没有则返回空字符串（防止 NPE）
        String exceptionStr = Optional.ofNullable(firstFieldError)
                // 在校验注解里写的 message 值（如 "用户名不能为空"）eg：@Min(value = 18, message = "年龄不能小于18")
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);
        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), exceptionStr);
        return Results.failure(BaseErrorCode.CLIENT_ERROR.code(), exceptionStr);
    }

    /**
     * 全局异常处理器：拦截项目中自定义的业务异常（继承自 AbstractException）
     *
     * @param request 当前 HTTP 请求对象（用于记录日志）
     * @param ex      抛出的自定义异常对象
     * @return 统一格式的错误响应（code + message）
     */
    @ExceptionHandler(value = {AbstractException.class})
    public Result abstractException(HttpServletRequest request, AbstractException ex) {
        // 当一个异常被“包装”成另一个异常时，你可以通过 .getCause() 找到“最初的错误
        if (ex.getCause() != null) {
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex.toString(), ex.getCause());
            return Results.failure(ex);
        }
        log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex.toString());
        return Results.failure(ex);
    }

    /**
     * 兜底拦截异常
     */
    @ExceptionHandler(value = Throwable.class) // 这是所有异常的父类
    public Result defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        return Results.failure();
    }

    /**
     * 获取当前请求的完整 URL（包含查询参数）
     *
     * @param request HTTP 请求对象
     * @return 完整的请求 URL 字符串，如：http://localhost:8080/user/list?name=张三&age=25
     */
    private String getUrl(HttpServletRequest request) {
        // request.getQueryString() 是获取当前 HTTP 请求中“URL 后面的查询参数字符串”，也就是 ? 后面那部分
        // 判断是否有查询参数，没有直接拼url
        if (StringUtils.isEmpty(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        // 有就返回完整的
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }
}
