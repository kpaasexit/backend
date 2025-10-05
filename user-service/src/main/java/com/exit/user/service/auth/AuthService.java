package com.exit.user.service.auth;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserDetailRequest;
import com.exit.common.exception.grpc.GrpcException;
import com.exit.user.controller.dto.request.OAuth2UserInfoRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.exit.user.domain.JwtToken;
import com.exit.user.domain.UserFcmToken;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserFcmTokenRepository;
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
    private final UserFcmTokenRepository userFcmTokenRepository;

    public LoginSuccessResponse socialLogin(OAuth2UserInfoRequestDto oauth2UserInfoRequestDto) {
        try {
            Users user = userRepository.findBySocialIdAndProvider(
                            oauth2UserInfoRequestDto.getSocialId(),
                            oauth2UserInfoRequestDto.getProvider()
                    )
                    .map(existingUser -> updateExistingUser(existingUser, oauth2UserInfoRequestDto))
                    .orElseGet(() -> createNewUser(oauth2UserInfoRequestDto));

            String deviceId = oauth2UserInfoRequestDto.getDeviceId();

            JwtToken jwtToken = createAndSaveJwtToken(user, deviceId);
            createAndSaveFcmToken(oauth2UserInfoRequestDto, user, deviceId);
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

            jwtTokenRedisService.validJwtToken(userId, deviceId, request.refreshToken());

            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

            jwtTokenRedisService.deleteJwtToken(userId, deviceId);

            JwtToken newJwtToken = createAndSaveJwtToken(user, deviceId);

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

    private JwtToken createAndSaveJwtToken(Users user, String deviceId) {
        String accessToken = jwtTokenProvider.generateAccessToken(new UserDetailRequest(user.getUserId(), deviceId));
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(new UserDetailRequest(user.getUserId(), deviceId), jti);

        // JWT 토큰을 Redis에 저장 (1일 만료)
        JwtToken jwtToken = JwtToken.builder()
                .jwtId(jti)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .expiresAt(System.currentTimeMillis() + Duration.ofDays(1).toMillis())
                .deviceId(deviceId)
                .build();

        jwtTokenRedisService.saveJwtToken(user.getUserId(), deviceId, jwtToken);
        return jwtToken;
    }

    private void createAndSaveFcmToken(OAuth2UserInfoRequestDto oauth2UserInfoRequestDto, Users user, String deviceId) {
        UserFcmToken userFcmToken = UserFcmToken.builder()
                .user(user)
                .token(oauth2UserInfoRequestDto.getFirebaseToken())
                .deviceId(deviceId)
                .active(true)
                .build();

        userFcmTokenRepository.save(userFcmToken);
    }
}
