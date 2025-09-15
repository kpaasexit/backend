package com.exit.user.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.user.domain.JwtToken;
import com.exit.user.exception.GrpcUserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtTokenRedisService {

    private static final String JWT_KEY_PREFIX = "jwt:";
    private static final Duration TOKEN_EXPIRATION = Duration.ofDays(1); // 1일 만료

    private final RedisTemplate<String, Object> redisTemplate;

    public void saveJwtToken(Long userId, JwtToken jwtToken) {
        String key = generateKey(userId);
        redisTemplate.opsForValue().set(key, jwtToken, TOKEN_EXPIRATION);
    }

    public JwtToken getJwtToken(Long userId) {
        String key = generateKey(userId);
        return (JwtToken) redisTemplate.opsForValue().get(key);
    }

    public void deleteJwtToken(Long userId) {
        String key = generateKey(userId);
        redisTemplate.delete(key);
    }

    public boolean existsJwtToken(Long userId) {
        String key = generateKey(userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setTokenExpiration(Long userId, long timeout, TimeUnit timeUnit) {
        String key = generateKey(userId);
        redisTemplate.expire(key, timeout, timeUnit);
    }

    private String generateKey(Long userId) {
        return JWT_KEY_PREFIX + userId;
    }

    public void validJwtToken(Long userId, String requestRefreshToken) {
        JwtToken storedToken = getJwtToken(userId);

        if (storedToken == null) {
            throw new GrpcException(GrpcUserErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 저장된 refresh token과 요청된 refresh token 비교
        if (!storedToken.getRefreshToken().equals(requestRefreshToken)) {
            throw new GrpcException(GrpcUserErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}