package com.monsoon.seedflowplus.domain.account.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTokenStore {

    private static final String PREFIX_REFRESH = "auth:refresh:";
    private static final String PREFIX_PWRESET = "auth:pwreset:";
    private static final String PREFIX_EMAIL_CODE = "auth:emailverify:code:";
    private static final String PREFIX_EMAIL_VERIFIED = "auth:emailverify:verified:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeRefreshToken(String loginId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(keyRefresh(loginId), token, ttl);
    }

    public String getRefreshToken(String loginId) {
        return redisTemplate.opsForValue().get(keyRefresh(loginId));
    }

    public void deleteRefreshToken(String loginId) {
        redisTemplate.delete(keyRefresh(loginId));
    }

    public void storePasswordResetCode(Long userId, String codeHash, Duration ttl) {
        redisTemplate.opsForValue().set(keyPasswordReset(userId), codeHash, ttl);
    }

    public String getPasswordResetCode(Long userId) {
        return redisTemplate.opsForValue().get(keyPasswordReset(userId));
    }

    public void deletePasswordResetCode(Long userId) {
        redisTemplate.delete(keyPasswordReset(userId));
    }

    public void storeEmailVerificationCode(String email, String codeHash, Duration ttl) {
        redisTemplate.opsForValue().set(keyEmailCode(email), codeHash, ttl);
    }

    public String getEmailVerificationCode(String email) {
        return redisTemplate.opsForValue().get(keyEmailCode(email));
    }

    public void deleteEmailVerificationCode(String email) {
        redisTemplate.delete(keyEmailCode(email));
    }

    public void storeEmailVerified(String email, Duration ttl) {
        redisTemplate.opsForValue().set(keyEmailVerified(email), "1", ttl);
    }

    public boolean isEmailVerified(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(keyEmailVerified(email)));
    }

    public void deleteEmailVerified(String email) {
        redisTemplate.delete(keyEmailVerified(email));
    }

    private String keyRefresh(String loginId) {
        return PREFIX_REFRESH + loginId;
    }

    private String keyPasswordReset(Long userId) {
        return PREFIX_PWRESET + userId;
    }

    private String keyEmailCode(String email) {
        return PREFIX_EMAIL_CODE + email;
    }

    private String keyEmailVerified(String email) {
        return PREFIX_EMAIL_VERIFIED + email;
    }
}
