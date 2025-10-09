package org.openzjl.index12306.framework.starter.idempotent.core.token;

import ch.qos.logback.core.net.server.Client;
import cn.hutool.core.util.StrUtil;
import com.google.common.base.Strings;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.openzjl.index12306.framework.starter.cache.DistributedCache;
import org.openzjl.index12306.framework.starter.convention.errorcode.BaseErrorCode;
import org.openzjl.index12306.framework.starter.convention.exception.ClientException;
import org.openzjl.index12306.framework.starter.idempotent.config.IdempotentProperties;
import org.openzjl.index12306.framework.starter.idempotent.core.AbstractIdempotentExecuteHandler;
import org.openzjl.index12306.framework.starter.idempotent.core.IdempotentParamWrapper;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * 基于 token 验证请求的幂等性，通常应用于重复提交请求的情况
 *
 * @author zhangjlk
 * @date 2025/10/6 15:42
 */
@RequiredArgsConstructor
public class IdempotentTokenExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentTokenService{

    /**
     * Redis缓存
     */
    private final DistributedCache distributedCache;

    /**
     * 自定义token幂等设置
     */
    private final IdempotentProperties idempotentProperties;

    private static final String TOKEN_KEY = "token";
    private static final String TOKEN_PREFIX_KEY = "token_prefix";
    private static final long TOKEN_EXPIRED_TIME = 6000;

    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        return new IdempotentParamWrapper();
    }

    @Override
    public String createToken() {
        // 格式：[前缀] + [随机UUID]
        String token = Optional.ofNullable(Strings.emptyToNull(idempotentProperties.getPrefix())).orElse(TOKEN_PREFIX_KEY) + UUID.randomUUID();
        // 将生成的 token 存入分布式缓存（如 Redis），用于后续幂等校验
        distributedCache.put(token, "", Optional.ofNullable(idempotentProperties.getTimout()).orElse(TOKEN_EXPIRED_TIME));
        return token;
    }

    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        /**
         * 使用下面这个去拿HttpServletRequest的背景：
         * 在 Spring 的 Controller 方法里，想用 request 对象很简单，直接作为方法参数就行：
         * @GetMapping("/test")
         * public String myControllerMethod(HttpServletRequest request) {
         *     // 这里直接用 request
         * }
         * 但如果你不在 Controller 里，而是在一个 Service 类或者一个普通的工具类里，
         * 也想拿到当前请求的信息（比如获取请求头、获取用户IP），怎么办？
         * 你总不能把 request 对象从 Controller 一路当做参数传到最深层的方法里吧，那样代码会很难看。
         *
         * Spring 框架早就考虑到了这个问题。
         * 它在背后默默地用一个和当前线程绑定的“存储空间”（ThreadLocal）存放了当前请求的所有信息。
         * 然后就像下面这样去拿
         *
         * RequestContextHolder: 这是 Spring 的一个核心工具类。专门用来存放当前请求的所有信息（比如请求头、参数、Session 等）。
         *
         * Spring 框架在处理每个 Web 请求时，会把该请求的所有信息（如 request、session 等）打包成一个叫 RequestAttributes 的对象
         *
         * ServletRequestAttributes 提供了 .getRequest() 方法，返回的就是我们熟悉的 HttpServletRequest 对象。
         */
        HttpServletRequest request =
                ((ServletRequestAttributes) (RequestContextHolder.currentRequestAttributes()))
                        .getRequest();

        // 拿到token
        String token = request.getHeader(TOKEN_KEY);
        if (StrUtil.isBlank(token)) {
            // 请求参数中获取token
            token = request.getParameter(TOKEN_KEY);
            if (StrUtil.isBlank(token)) {
                // 如果两种方式都找不到 Token，说明客户端没有按规定传递，直接抛出异常
                throw new ClientException(BaseErrorCode.IDEMPOTENT_TOKEN_DELETE_ERROR);
            }
        }
        /**
         * 使用分布式缓存（如 Redis）来验证 Token 的有效性。
         * distributedCache.delete(token) 会尝试删除以 token 为键的缓存。
         * 这是一个原子操作：
         *  - 如果删除成功，说明 token 存在且是第一次被使用，返回 true。
         *  - 如果删除失败（缓存里根本没有这个 key），说明 token 是伪造的或已被使用过，返回 false。
         */
        Boolean tokenDelFlag = distributedCache.delete(token);
        if (!tokenDelFlag) {
            String errorMsg = StrUtil.isNotBlank(wrapper.getIdempotent().message())
                    ? wrapper.getIdempotent().message()
                    :BaseErrorCode.IDEMPOTENT_TOKEN_DELETE_ERROR.message();
            throw new ClientException(errorMsg, BaseErrorCode.IDEMPOTENT_TOKEN_DELETE_ERROR);
        }
    }
}
