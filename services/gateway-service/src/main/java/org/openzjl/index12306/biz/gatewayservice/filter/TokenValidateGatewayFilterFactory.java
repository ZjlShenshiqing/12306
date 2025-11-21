package org.openzjl.index12306.biz.gatewayservice.filter;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import org.openzjl.index12306.biz.gatewayservice.config.Config;
import org.openzjl.index12306.biz.gatewayservice.toolkit.JWTUtil;
import org.openzjl.index12306.biz.gatewayservice.toolkit.UserInfoDTO;
import org.openzjl.index12306.framework.starter.bases.constant.UserConstant;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * SpringCloud Gateway Token拦截器
 *
 * @author zhangjlk
 * @date 2025/10/12 16:59
 */
@Component
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {

    public TokenValidateGatewayFilterFactory() {
        super(Config.class);
    }

    /**
     * 注销用户的时候需要传递token
     */
    public static final String DELETION_PATH = "/api/user-service/deletion";

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            // 获取本次请求的完整路径，例如：/api/user-service/deletion
            String requestPath = request.getPath().toString();

            // 判断当前请求路径，是否命中「需要校验 Token 的路径前缀」黑名单
            if (isPathInBlackPreList(requestPath, config.getBlackPathPre())) {

                // 如果命中了黑名单前缀，说明这个接口必须带上 Authorization 头里的 Token
                // 从请求头中取出第一个名为 "Authorization" 的值
                String token = request.getHeaders().getFirst("Authorization");

                // TODO: 这里应该加上对 token 是否为空、格式是否正确的检查
                // TODO: 同时还需要验证 Token 是否仍然有效：
                //       比如用户已经注销 / 被拉黑，但 token 的过期时间还没到，这种也应该视为无效

                // 使用工具类解析 JWT，拿到其中包含的用户信息
                UserInfoDTO userInfoDTO = JWTUtil.parseJwtToken(token);

                // 进一步校验 token 对应的用户信息是否有效
                if (!validateToken(userInfoDTO)) {
                    // 如果校验失败，则构造一个 401 Unauthorized 响应，拒绝本次请求

                    // 从 exchange 中拿到响应对象
                    ServerHttpResponse response = exchange.getResponse();
                    // 设置 HTTP 状态码为 401 未授权
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    // 终止过滤链，不再继续往后面的过滤器 / 业务服务传递
                    return response.setComplete();
                }


                // 从当前请求 exchange 中拿到原始 request，基于它创建一个可修改的 Builder
                ServerHttpRequest.Builder builder = exchange.getRequest()
                        .mutate()                      // 复制一份 request，得到一个可变的构建器
                        .headers(httpHeaders -> {      // 在这一步里统一修改 / 添加请求头

                            // 在请求头中写入用户ID
                            httpHeaders.set(UserConstant.USER_ID_KEY, userInfoDTO.getUserId());

                            // 在请求头中写入用户名（登录名）
                            httpHeaders.set(UserConstant.USER_NAME_KEY, userInfoDTO.getUsername());

                            // 在请求头中写入真实姓名，为防止中文/特殊字符问题，先用 UTF-8 URL 编码
                            httpHeaders.set(
                                    UserConstant.REAL_NAME_KEY,
                                    URLEncoder.encode(userInfoDTO.getRealName(), StandardCharsets.UTF_8)
                            );

                            // 如果当前请求路径是“删除”接口，就额外把 token 也塞进请求头里
                            if (Objects.equals(requestPath, DELETION_PATH)) {
                                httpHeaders.set(UserConstant.USER_TOKEN_KEY, token);
                            }
                        });

                // 用加好头的 request 替换掉原来的 request，继续往下游过滤器 / 业务逻辑传递
                return chain.filter(
                        exchange.mutate()              // 基于原来的 exchange 创建一个新的可变副本
                                .request(builder.build()) // 把刚刚构建好的新 request 放进去
                                .build()                 // 构建出新的 ServerWebExchange
                );
            }

            // 8. 如果没命中黑名单路径，或者 token 校验通过，则放行到下一个过滤器 / 目标服务
            return chain.filter(exchange);
        };
    }

    /**
     * 判断当前请求路径 requestPath 是否以黑名单前缀列表中的任意一个前缀开头
     *
     * 举例：
     *   blackPathPre = ["/inner", "/admin"]
     *   requestPath = "/admin/user/list"  -> 返回 true
     *   requestPath = "/api/user"         -> 返回 false
     *
     * @param requestPath  当前请求的路径，比如 "/api/user-service/login"
     * @param blackPathPre 黑名单路径前缀列表，比如 ["/admin", "/inner"]
     * @return 如果 requestPath 以列表中任意一个前缀开头，则返回 true，否则返回 false
     */
    private boolean isPathInBlackPreList(String requestPath, List<String> blackPathPre) {
        if (CollectionUtils.isEmpty(blackPathPre)) {
            return false;
        }
        // 对 blackPathPre 中的每一个前缀 prefix，检查 requestPath.startsWith(prefix)
        // 只要有一个前缀匹配，就返回 true；否则返回 false
        return blackPathPre.stream().anyMatch(requestPath::startsWith);
    }

    /**
     * 验证token
     *
     * @param userInfoDTO 用户信息实体
     * @return token是否存在
     */
    private boolean validateToken(UserInfoDTO userInfoDTO) {
        return userInfoDTO != null;
    }
}
