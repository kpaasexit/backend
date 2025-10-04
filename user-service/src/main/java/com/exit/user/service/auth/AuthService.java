package com.exit.user.service.auth;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserIdRequest;
import com.exit.common.exception.grpc.GrpcException;
import com.exit.user.controller.dto.request.OAuth2UserInfoRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.exit.user.domain.DeviceType;
import com.exit.user.domain.JwtToken;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserRepository;
import com.exit.user.exception.GrpcUserErrorCode;
import com.exit.user.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenRedisService jwtTokenRedisService;

    public LoginSuccessResponse socialLogin(OAuth2UserInfoRequestDto oauth2UserInfoRequestDto) {
        try {
            Users user = userRepository.findBySocialIdAndProvider(
                            oauth2UserInfoRequestDto.getSocialId(),
                            oauth2UserInfoRequestDto.getProvider()
                    )
                    .map(existingUser -> updateExistingUser(existingUser, oauth2UserInfoRequestDto))
                    .orElseGet(() -> createNewUser(oauth2UserInfoRequestDto));

            String deviceId = oauth2UserInfoRequestDto.getDeviceId();

            JwtToken jwtToken = createJwtToken(user, deviceId, oauth2UserInfoRequestDto.getDeviceType());
            jwtTokenRedisService.saveJwtToken(user.getUserId(), deviceId, jwtToken);

            return new LoginSuccessResponse(
                    jwtToken.getAccessToken(),
                    jwtToken.getRefreshToken(),
                    user.getUserId(),
                    user.getUserProfileUrl()
            );

        } catch (Exception e) {
            throw new GrpcException(GrpcUserErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }

    public LoginSuccessResponse refreshAuthToken(RefreshTokenRequestDto request) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());
            String deviceId = request.deviceId();

            if (deviceId == null || deviceId.isEmpty()) {
                throw new GrpcException(GrpcUserErrorCode.INVALID_REFRESH_TOKEN);
            }

            jwtTokenRedisService.validJwtToken(userId, deviceId, request.refreshToken());

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

            JwtToken oldToken = jwtTokenRedisService.getJwtToken(userId, deviceId);
            DeviceType deviceType = oldToken.getDeviceType();

            JwtToken newJwtToken = createJwtToken(user, deviceId, deviceType);
            jwtTokenRedisService.saveJwtToken(userId, deviceId, newJwtToken);

            return new LoginSuccessResponse(
                    newJwtToken.getAccessToken(),
                    newJwtToken.getRefreshToken(),
                    user.getUserId(),
                    user.getUserProfileUrl()
            );

        } catch (Exception e) {
            throw new GrpcException(GrpcUserErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public LoginSuccessResponse logout(Long userId, String deviceId) {
        try {
            if (deviceId == null || deviceId.isEmpty()) {
                // deviceId가 없으면 모든 디바이스 로그아웃
                jwtTokenRedisService.deleteAllJwtTokens(userId);
            } else {
                // 특정 디바이스만 로그아웃
                jwtTokenRedisService.deleteJwtToken(userId, deviceId);
            }

            return new LoginSuccessResponse(
                    null,
                    null,
                    userId,
                    null
            );

        } catch (Exception e) {
            throw new GrpcException(GrpcUserErrorCode.LOGOUT_FAILED);
        }
    }

    private Users updateExistingUser(Users user, OAuth2UserInfoRequestDto dto) {
        user.updateProfile(dto.getName(), dto.getProfileImageUrl());
        return user;
    }

    private Users createNewUser(OAuth2UserInfoRequestDto dto) {
        String nickname = NicknameGenerator.generate();

        return userRepository.save(Users.builder()
                .userEmail(dto.getEmail())
                .userNickname(nickname)
                .userProfileUrl(dto.getProfileImageUrl())
                .socialId(dto.getSocialId())
                .provider(dto.getProvider())
                .build());
    }

    private JwtToken createJwtToken(Users user, String deviceId, DeviceType deviceType) {
        String accessToken = jwtTokenProvider.generateAccessToken(new UserIdRequest(user.getUserId()));
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(new UserIdRequest(user.getUserId()), jti);

        // JWT 토큰을 Redis에 저장 (1일 만료)
        return JwtToken.builder()
                .jwtId(jti)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .expiresAt(System.currentTimeMillis() + Duration.ofDays(1).toMillis())
                .deviceId(deviceId)
                .deviceType(deviceType != null ? deviceType : DeviceType.WEB)
                .build();
    }
}
