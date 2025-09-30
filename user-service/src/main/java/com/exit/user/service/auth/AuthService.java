package com.exit.user.service.auth;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.auth.jwt.dto.UserIdRequest;
import com.exit.common.exception.grpc.GrpcException;
import com.exit.user.controller.dto.request.OAuth2UserInfoRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.exit.user.domain.JwtToken;
import com.exit.user.domain.Users;
import com.exit.user.domain.repository.UserRepository;
import com.exit.user.exception.GrpcUserErrorCode;
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


            // JWT 토큰 생성
            JwtToken jwtToken = createJwtToken(user);
            jwtTokenRedisService.saveJwtToken(user.getUserId(), jwtToken);

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
            // Refresh Token에서 사용자 ID 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken());

            // JWT 토큰 유효성 검증
            jwtTokenRedisService.validJwtToken(userId, request.refreshToken());

            // 사용자 정보 조회
            Users user = userRepository.findById(userId)
                    .orElseThrow(() -> new GrpcException(GrpcUserErrorCode.USER_NOT_FOUND));

            JwtToken newJwtToken = createJwtToken(user);
            jwtTokenRedisService.saveJwtToken(userId, newJwtToken);

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

    public LoginSuccessResponse logout(UserIdRequest request) {
        try {
            // Redis에서 JWT 토큰 삭제
            jwtTokenRedisService.deleteJwtToken(request.userId());

            return new LoginSuccessResponse(
                    null,
                    null,
                    request.userId(),
                    null
            );

        } catch (Exception e) {
            throw new GrpcException(GrpcUserErrorCode.LOGOUT_FAILED);
        }
    }

    // 헬퍼 메서드들
    private Users updateExistingUser(Users user, OAuth2UserInfoRequestDto dto) {
        user.updateProfile(dto.getName(), dto.getProfileImageUrl());
        return user;
    }

    private Users createNewUser(OAuth2UserInfoRequestDto dto) {
        return userRepository.save(Users.builder()
                .userEmail(dto.getEmail())
                .userNickname(dto.getName())
                .userProfileUrl(dto.getProfileImageUrl())
                .socialId(dto.getSocialId())
                .provider(dto.getProvider())
                .build());
    }

    private JwtToken createJwtToken(Users user) {
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(new UserIdRequest(user.getUserId()));
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtTokenProvider.generateRefreshToken(new UserIdRequest(user.getUserId()), jti);

        // JWT 토큰을 Redis에 저장 (1일 만료)
        return JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getUserId())
                .expiresAt(System.currentTimeMillis() + Duration.ofDays(1).toMillis())
                .build();
    }
}
