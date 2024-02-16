package io.github.xxyopen.novel.manager.redis;

import io.github.xxyopen.novel.core.common.util.ImgVerifyCodeUtils;
import io.github.xxyopen.novel.core.constant.CacheConsts;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 验证码 管理类
 *
 * @author hedong
 * @date 2022/5/12
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerifyCodeManager {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成图形验证码，并放入 Redis 中
     *
     * 注意：我们在保存验证码的时候需要一个全局唯一的sessionId字符串用于标识该验证码属于哪个浏览器会话，
     * 该 sessionId 会在验证码返回给前端的时候一并返回，在用户提交注册的时候，该sessionId会和验证码一起提交用于校验。
     *
     * redisTemplate.opsForValue();//操作字符串
     * redisTemplate.opsForHash();//操作hash
     * redisTemplate.opsForList();//操作list
     * redisTemplate.opsForSet();//操作set
     * redisTemplate.opsForZSet();//操作有序set
     */

    public String genImgVerifyCode(String sessionId) throws IOException {
        String verifyCode = ImgVerifyCodeUtils.getRandomVerifyCode(4); // 生成验证码
        String img = ImgVerifyCodeUtils.genVerifyCodeImg(verifyCode); // 生成图形验证码
        // stringRedisTemplate.opsForValue().set(K key, V value, Duration timeout)
        // 这里的key和value（都是字符串）："前缀 + IMG_VERIFY_CODE_CACHE_KEY + sessionId"；value：verifyCode
        stringRedisTemplate.opsForValue().set(CacheConsts.IMG_VERIFY_CODE_CACHE_KEY + sessionId,
            verifyCode, Duration.ofMinutes(5)); // 写入redis缓存
        return img;
    }

    /**
     * 校验图形验证码
     */
    public boolean imgVerifyCodeOk(String sessionId, String verifyCode) {
        // 通过缓存判断缓存中的验证码与后台生成验证码是否一致
        return Objects.equals(stringRedisTemplate.opsForValue()
            .get(CacheConsts.IMG_VERIFY_CODE_CACHE_KEY + sessionId), verifyCode);
    }

    /**
     * 从 Redis 中删除验证码
     */
    public void removeImgVerifyCode(String sessionId) {
        stringRedisTemplate.delete(CacheConsts.IMG_VERIFY_CODE_CACHE_KEY + sessionId);
    }

}
