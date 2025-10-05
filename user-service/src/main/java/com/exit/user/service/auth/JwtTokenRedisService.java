package com.exit.user.service.auth;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.user.domain.JwtToken;
import com.exit.user.exception.GrpcUserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtTokenRedisService {

    private static final String JWT_KEY_PREFIX = "jwt:";
    private static final String USER_DEVICES_PREFIX = "user_devices:";
    private static final Duration TOKEN_EXPIRATION = Duration.ofDays(1);
    private static final int MAX_DEVICES = 5;
    private final RedisTemplate<String, Object> redisTemplate;

    public void saveJwtToken(Long userId, String deviceId, JwtToken jwtToken) {
        // jwt:{userId}:{deviceId} 형태로 토큰 저장
        String tokenKey = generateDeviceTokenKey(userId, deviceId);
        redisTemplate.opsForValue().set(tokenKey, jwtToken, TOKEN_EXPIRATION);

        // user_devices:{userId} Set에 deviceId 추가
        String devicesKey = generateUserDevicesKey(userId);
        redisTemplate.opsForSet().add(devicesKey, deviceId);
        redisTemplate.expire(devicesKey, TOKEN_EXPIRATION);

        // 최대 디바이스 수 초과 시 가장 오래된 디바이스 제거
        enforceMaxDevices(userId);
    }

    public JwtToken getJwtToken(Long userId, String deviceId) {
        String key = generateDeviceTokenKey(userId, deviceId);
        return (JwtToken) redisTemplate.opsForValue().get(key);
    }

    public List<JwtToken> getAllJwtTokens(Long userId) {
        Set<String> deviceIds = getUserDevices(userId);
        List<JwtToken> tokens = new ArrayList<>();

        for (String deviceId : deviceIds) {
            JwtToken token = getJwtToken(userId, deviceId);
            if (token != null) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    public Set<String> getUserDevices(Long userId) {
        String devicesKey = generateUserDevicesKey(userId);
        Set<Object> devices = redisTemplate.opsForSet().members(devicesKey);

        if (devices == null) {
            return new HashSet<>();
        }

        return devices.stream()
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    public void deleteJwtToken(Long userId, String deviceId) {
        String tokenKey = generateDeviceTokenKey(userId, deviceId);
        redisTemplate.delete(tokenKey);

        String devicesKey = generateUserDevicesKey(userId);
        redisTemplate.opsForSet().remove(devicesKey, deviceId);
    }

    public void deleteAllJwtTokens(Long userId) {
        Set<String> deviceIds = getUserDevices(userId);

        for (String deviceId : deviceIds) {
            String tokenKey = generateDeviceTokenKey(userId, deviceId);
            redisTemplate.delete(tokenKey);
        }

        String devicesKey = generateUserDevicesKey(userId);
        redisTemplate.delete(devicesKey);
    }

    public boolean existsJwtToken(Long userId, String deviceId) {
        String key = generateDeviceTokenKey(userId, deviceId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void setTokenExpiration(Long userId, String deviceId, long timeout, TimeUnit timeUnit) {
        String key = generateDeviceTokenKey(userId, deviceId);
        redisTemplate.expire(key, timeout, timeUnit);
    }

    public void validJwtToken(Long userId, String deviceId, String requestRefreshToken) {
        JwtToken storedToken = getJwtToken(userId, deviceId);

        if (storedToken == null) {
            throw new GrpcException(GrpcUserErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 저장된 refresh token과 요청된 refresh token 비교
        if (!storedToken.getRefreshToken().equals(requestRefreshToken)) {
            throw new GrpcException(GrpcUserErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private void enforceMaxDevices(Long userId) {
        Set<String> deviceIds = getUserDevices(userId);
        if (deviceIds.size() > MAX_DEVICES) {
            // 가장 오래된 디바이스 찾기 (expiresAt 기준)
            String oldestDeviceId = null;
            Long oldestExpiresAt = Long.MAX_VALUE;
            for (String deviceId : deviceIds) {
                JwtToken token = getJwtToken(userId, deviceId);
                if (token != null && token.getExpiresAt() < oldestExpiresAt) {
                    oldestExpiresAt = token.getExpiresAt();
                    oldestDeviceId = deviceId;
                }
            }
            // 가장 오래된 디바이스 삭제
            if (oldestDeviceId != null) {
                deleteJwtToken(userId, oldestDeviceId);
            }
        }
    }

    private String generateDeviceTokenKey(Long userId, String deviceId) {
        return JWT_KEY_PREFIX + userId + ":" + deviceId;
    }

    private String generateUserDevicesKey(Long userId) {
        return USER_DEVICES_PREFIX + userId;
    }
}