/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.job;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 定时/运维任务参数：从 HTTP 请求头 {@code requestParam} 或查询参数 {@code requestParam} 读取。
 */
public final class JobRequestParamUtil {

    private JobRequestParamUtil() {
    }

    public static String resolve() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        String header = req.getHeader("requestParam");
        if (StrUtil.isNotBlank(header)) {
            return header;
        }
        String param = req.getParameter("requestParam");
        return StrUtil.isNotBlank(param) ? param : null;
    }
}
