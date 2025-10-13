package org.openzjl.index12306.biz.gatewayservice.toolkit;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import org.openzjl.index12306.biz.gatewayservice.toolkit.UserInfoDTO;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.openzjl.index12306.framework.starter.bases.constant.UserConstant.*;

/**
 * jwt工具类
 * @author zhangjlk
 * @date 2025/9/16 20:43
 */
@Slf4j
public class JWTUtil {
    // JWT 令牌的有效期
    private static final long EXPIRATION = 86400L;
    // HTTP Authorization 头中 JWT 令牌的标准前缀
    public static final String TOKEN_PREFIX = "Bearer ";
    // JWT 的签发者，表示谁创建了这个令牌
    public static final String ISS = "index12306";
    // 用于签名和验证 JWT 的密钥
    public static final String SECRET = "SecretKey039245678901232039487623456783092349288901402967890140939827";

    /**
     * 生成用户token
     * @param userInfoDTO 用户信息
     * @return token
     */
    public static String generateAccessToken(UserInfoDTO userInfoDTO) {
        Map<String, Object> customerUserMap = new HashMap<>();
        customerUserMap.put(USER_ID_KEY, userInfoDTO.getUserId());
        customerUserMap.put(USER_NAME_KEY, userInfoDTO.getUsername());
        customerUserMap.put(REAL_NAME_KEY, userInfoDTO.getRealName());
        String jwtToken = Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECRET)
                // .setIssuedAt(...) → 设置签发时间
                .setIssuedAt(new Date())
                .setIssuer(ISS)
                // 令牌的主题（通常是用户身份）
                .setSubject(JSON.toJSONString(customerUserMap))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION * 1000))
                // 生成最终的 JWT 字符串
                .compact();
        return TOKEN_PREFIX + jwtToken;
    }

    /**
     * 解析用户Token
     * @param jwtToken 用户token
     * @return 用户信息
     */
    public static UserInfoDTO parseJwtToken(String jwtToken) {
        if (StringUtils.hasText(jwtToken)) {
            String actualJwtToken = jwtToken.replace(TOKEN_PREFIX, "");
            try {
                /**
                 * eyJhbGci... . eyJzdWIiOiJ7InVzZXIiLCJ6aGFuZ3NhbiJ9... . xxxxxx
                 *    ↑             ↑                              ↑
                 *   头部          载荷（payload）                签名
                 *
                 * 现在拿到的是payload载荷
                 * Payload 是一组“声明”（Claims）
                 * ayload 是一组“声明”（Claims），常见的有：
                 * subject 主体，通常是用户相关数据
                 *
                 * expiration 过期时间（时间戳）
                 *
                 * issuer 发行者
                 *
                 * issued at 签发时间
                 */
                Claims claims = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(actualJwtToken).getBody();
                Date expiration = claims.getExpiration();
                if (expiration.after(new Date())) {
                    String subject = claims.getSubject();
                    return JSON.parseObject(subject, UserInfoDTO.class);
                }
                // 如果这个 token 已经过期了，就“安静地忽略它”，不报错也不记录日志，直接跳过去
            } catch (ExpiredJwtException ignored) {
            } catch (Exception ex) {
                log.error("JWT Token解析失败", ex);
            }
        }
        return null;
    }
}
