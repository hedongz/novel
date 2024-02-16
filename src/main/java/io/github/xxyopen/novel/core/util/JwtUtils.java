package io.github.xxyopen.novel.core.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * JWT 工具类
 *
 * @author xiongxiaoyang
 * @date 2022/5/17
 */
@ConditionalOnProperty("novel.jwt.secret")
@Component
@Slf4j
public class JwtUtils {

    /**
     * 注入JWT加密密钥
     */
    @Value("${novel.jwt.secret}")
    private String secret;

    /**
     * 定义系统标识头常量
     */
    private static final String HEADER_SYSTEM_KEY = "systemKeyHeader";

    /**
     * 根据用户ID生成JWT
     *
     * @param uid       用户ID
     * @param systemKey 系统标识
     * @return JWT
     */
    public String generateToken(Long uid, String systemKey) {
        // Keys.hmacShaKeyFor(byte[]) 内部也是使用 new SecretKeySpec(bytes, alg.getJcaName()) 来生成密钥的
        // ****根据key生成密钥（会根据字节参数长度自动选择相应的 HMAC 算法）****
        // HS256: HMAC using SHA-256
        // HS384: HMAC using SHA-384
        // HS512: HMAC using SHA-512
        // 以上都是对称加密算法
        return Jwts.builder()
            .setHeaderParam(HEADER_SYSTEM_KEY, systemKey) // 设置头参数
            .setSubject(uid.toString()) // 设置主题为uid
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))) // 使用密钥对JWT签名，防止篡改
            .compact();
    }

    /**
     * 解析JWT返回用户ID
     *
     * @param token     JWT
     * @param systemKey 系统标识
     * @return 用户ID
     */
    public Long parseToken(String token, String systemKey) {
        Jws<Claims> claimsJws;
        try {
            claimsJws = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token);
            // OK, we can trust this JWT
            // 判断该 JWT 是否属于指定系统
            if (Objects.equals(claimsJws.getHeader().get(HEADER_SYSTEM_KEY), systemKey)) {
                return Long.parseLong(claimsJws.getBody().getSubject());
            }
        } catch (JwtException e) {
            log.warn("JWT解析失败:{}", token);
            // don't trust the JWT!
        }
        return null;
    }
}
