package com.exit.user.service.auth;

import com.exit.common.grpc.*;
import com.exit.user.controller.dto.request.OAuth2UserInfoRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    @Override
    public void socialLogin(SocialLoginRequest request,
                            StreamObserver<SocialLoginResponse> responseObserver) {
        log.info("Social login with user info request received for provider: {}", request.getProvider());
        OAuth2UserInfoRequestDto oauth2UserInfoRequestDto = OAuth2UserInfoRequestDto.builder()
                .socialId(request.getSocialId())
                .email(request.getEmail())
                .name(request.getName())
                .provider(request.getProvider())
                .deviceId(request.getDeviceId())
                .build();

        LoginSuccessResponse loginResponse = authService.socialLogin(oauth2UserInfoRequestDto);

        SocialLoginResponse.Builder builder = SocialLoginResponse.newBuilder()
                .setAccessToken(loginResponse.accessToken())
                .setRefreshToken(loginResponse.refreshToken())
                .setUserId(loginResponse.userId())
                .setEmail(oauth2UserInfoRequestDto.getEmail())
                .setNickname(oauth2UserInfoRequestDto.getName())
                .setProvider(oauth2UserInfoRequestDto.getProvider())
                .setSocialId(oauth2UserInfoRequestDto.getSocialId());

        if (loginResponse.profileImageUrl() != null) {
            builder.setProfileImageUrl(loginResponse.profileImageUrl());
        }

        SocialLoginResponse grpcResponse = builder.build();

        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        log.info("Refresh token request received");

        RefreshTokenRequestDto refreshDto = RefreshTokenRequestDto.from(request);

        LoginSuccessResponse serviceResponse = authService.refreshAuthToken(refreshDto);

        RefreshTokenResponse grpcResponse = RefreshTokenResponse.newBuilder()
                .setAccessToken(serviceResponse.accessToken())
                .setRefreshToken(serviceResponse.refreshToken())
                .setExpiresIn(3600L) // 1시간
                .build();

        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        log.info("Logout request received for userId: {}, deviceId: {}", request.getUserId(), request.getDeviceId());
        authService.logout(request.getUserId(), request.getDeviceId());

        LogoutResponse grpcResponse = LogoutResponse.newBuilder()
                .setSuccess(true)
                .setMessage("로그아웃이 완료되었습니다")
                .build();

        responseObserver.onNext(grpcResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void withdraw(WithDrawRequest request, StreamObserver<Empty> responseObserver) {
        log.info("withdraw request received for userId: {}", request.getUserId());
        authService.withdraw(request.getUserId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}