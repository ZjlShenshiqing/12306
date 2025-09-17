package org.openzjl.index12306.framework.starter.user.core;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.openzjl.index12306.framework.starter.bases.constant.UserConstant;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;

/**
 * 用户信息传输过滤器
 *
 * 添加用户上下文过滤器，如果 HTTP 请求 Header 中包含用户信息，则进行解析并放入 `UserContext`。
 *
 * @author zhangjlk
 * @date 2025/9/16 20:43
 */
public class UserTransmitFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String userId = httpServletRequest.getHeader(UserConstant.USER_ID_KEY);
        if (StringUtils.hasText(userId)) {
            String userName = httpServletRequest.getHeader(UserConstant.USER_NAME_KEY);
            String realName = httpServletRequest.getHeader(UserConstant.REAL_NAME_KEY);
            if (StringUtils.hasText(userName)) {
                // 如果 userName 不是 null 也不是空字符串，就对它进行 URL 解码，把像 %E5%BC%A0%E4%B8%89 这样的编码转回成正常的中文或特殊字符（如“张三”）。
                userName = URLDecoder.decode(userName, "UTF-8");
            }

            if (StringUtils.hasText(realName)) {
                realName = URLDecoder.decode(realName, "UTF-8");
            }

            String token = httpServletRequest.getHeader(UserConstant.USER_TOKEN_KEY);
            UserInfoDTO userInfoDTO = UserInfoDTO.builder()
                    .userId(userId)
                    .username(userName)
                    .realName(realName)
                    .token(token)
                    .build();
            UserContext.setUser(userInfoDTO);
        }
        try {
            filterChain.doFilter(servletRequest, servletResponse); // 放行
        } finally {
            UserContext.removeUser(); // 请求级上下文（Request-scoped Context）要移除用户
        }
    }
}
