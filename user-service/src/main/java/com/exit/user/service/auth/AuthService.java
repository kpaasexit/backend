package com.exit.user.service.auth;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.util.file.FileUploadUtil;
import com.exit.user.controller.dto.request.OAuth2UserInfoRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.exit.user.domain.JwtToken;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserRepository;
import com.exit.user.exception.GrpcAuthErrorCode;
import com.exit.user.exception.GrpcUserErrorCode;
import com.exit.user.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenRedisService jwtTokenRedisService;
    private final FileUploadUtil fileUploadUtil;

    public LoginSuccessResponse socialLogin(OAuth2UserInfoRequestDto oauth2UserInfoRequestDto) {
        try {
            Users user = userRepository.findBySocialIdAndProviderAndUserDeletedFalse(oauth2UserInfoRequestDto.getSocialId(), oauth2UserInfoRequestDto.getProvider()).orElseGet(() -> createNewUser(oauth2UserInfoRequestDto));

            String deviceId = oauth2UserInfoRequestDto.getDeviceId();

            JwtToken jwtToken = createAndSaveJwtToken(user, deviceId);
            return new LoginSuccessResponse(jwtToken.getAccessToken(), jwtToken.getRefreshToken(), user.getUserId(), user.getUserProfileUrl());
        } catch (GrpcException e) {
            throw e;
        } catch (org.springframework.dao.DataAccessException e) {
            throw new GrpcException(GrpcUserErrorCode.DB_CONNECTION_FAILED);
        } catch (Exception e) {
            throw new GrpcException(GrpcAuthErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }

    public LoginSuccessResponse refreshAuthToken(RefreshTokenRequestDto request) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
            String deviceId = request.deviceId();

            jwtTokenRedisService.validJwtToken(userId, deviceId, request.refreshToken());

            Users user = getUserById(userId);

            jwtTokenRedisService.deleteJwtToken(userId, deviceId);

            JwtToken newJwtToken = createAndSaveJwtToken(user, deviceId);

            return new LoginSuccessResponse(newJwtToken.getAccessToken(), newJwtToken.getRefreshToken(), user.getUserId(), user.getUserProfileUrl());
        } catch (GrpcException e) {
            throw e;
        } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
            throw new GrpcException(GrpcUserErrorCode.REDIS_CONNECTION_FAILED);
        } catch (org.springframework.dao.DataAccessException e) {
            throw new GrpcException(GrpcUserErrorCode.DB_CONNECTION_FAILED);
        } catch (Exception e) {
            throw new GrpcException(GrpcAuthErrorCode.REFRESH_TOKEN_FAILED);
        }
    }

    public LoginSuccessResponse logout(Long userId, String deviceId) {
        try {
            if (deviceId == null || deviceId.isEmpty()) {
                jwtTokenRedisService.deleteAllJwtTokens(userId);
            } else {
                jwtTokenRedisService.deleteJwtToken(userId, deviceId);
            }

            return new LoginSuccessResponse(null, null, userId, null);
        } catch (org.springframework.data.redis.RedisConnectionFailureException e) {
            throw new GrpcException(GrpcUserErrorCode.REDIS_CONNECTION_FAILED);
        } catch (Exception e) {
            throw new GrpcException(GrpcAuthErrorCode.LOGOUT_FAILED);
        }
    }

    private Users createNewUser(OAuth2UserInfoRequestDto dto) {
        try {
            String nickname = NicknameGenerator.generate();
            return userRepository.save(
                    Users.builder()
                            .userEmail(dto.getEmail())
                            .userNickname(nickname)
                            .userProfileUrl(dto.getProfileImageUrl())
                            .socialId(dto.getSocialId())
                            .provider(dto.getProvider())
                            .build());
        } catch (Exception e) {
            throw new GrpcException(GrpcAuthErrorCode.USER_CREATION_FAILED);
        }
    }

    private JwtToken createAndSaveJwtToken(Users user, String deviceId) {
        String accessToken = jwtTokenProvider.generateAccessToken(new UserDetailRequest(user.getUserId(), deviceId));
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(new UserDetailRequest(user.getUserId(), deviceId), jti);

        JwtToken jwtToken = JwtToken.builder().jwtId(jti).accessToken(accessToken).refreshToken(refreshToken).userId(user.getUserId()).expiresAt(System.currentTimeMillis() + Duration.ofDays(1).toMillis()).deviceId(deviceId).build();

        jwtTokenRedisService.saveJwtToken(user.getUserId(), deviceId, jwtToken);
        return jwtToken;
    }

    public void withdraw(Long userId) {
        try {
            try {
                jwtTokenRedisService.deleteAllJwtTokens(userId);
            } catch (Exception e) {
                log.error("Exception while deleting jwt token", e);
            }

            Users user = getUserById(userId);

            if (user.getUserProfileUrl() != null && !user.getUserProfileUrl().isEmpty()) {
                try {
                    fileUploadUtil.deleteFile(user.getUserProfileUrl());
                } catch (Exception e) {
                    log.error("Exception while deleting user profile url", e);
                }
            }
            user.withdraw();
            userRepository.save(user);

        } catch (GrpcException e) {
            throw e;
        } catch (org.springframework.dao.DataAccessException e) {
            throw new GrpcException(GrpcUserErrorCode.DB_CONNECTION_FAILED);
        } catch (Exception e) {
            throw new GrpcException(GrpcAuthErrorCode.WITHDRAW_FAILED);
        }
    }

    private Users getUserById(Long userId) {
        Users user = userRepository.findById(userId).orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));
        if (user.isUserDeleted()) {
            throw new GrpcException(GrpcUserErrorCode.USER_ALREADY_DELETED);
        }
        return user;
    }
}