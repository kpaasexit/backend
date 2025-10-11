package com.exit.user.service.auth;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.user.controller.dto.request.OAuth2UserInfoRequestDto;
import com.exit.user.controller.dto.request.RefreshTokenRequestDto;
import com.exit.user.controller.dto.response.LoginSuccessResponse;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final AuthService authService;

    public void socialLogin(SocialLoginRequest request,
                            StreamObserver<SocialLoginResponse> responseObserver) {
        try {
            log.info("Social login with user info request received for provider: {}", request.getProvider());
            // SocialLoginWithUserInfoRequest를 OAuth2UserInfo로 변환
            OAuth2UserInfoRequestDto oauth2UserInfoRequestDto = OAuth2UserInfoRequestDto.builder()
                    .socialId(request.getSocialId())
                    .email(request.getEmail())
                    .name(request.getName())
                    .provider(request.getProvider())
                    .deviceId(request.getDeviceId())
                    .build();

            // 소셜 로그인 처리 (회원가입 or 로그인)
            LoginSuccessResponse loginResponse = authService.socialLogin(oauth2UserInfoRequestDto);

            // gRPC 응답 생성
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

        } catch (Exception e) {
            log.error("Social login with user info failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("소셜 로그인 처리 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<RefreshTokenResponse> responseObserver) {
        try {
            log.info("Refresh token request received");

            // 기존 서비스 호출
            RefreshTokenRequestDto refreshDto = RefreshTokenRequestDto.from(request);

            LoginSuccessResponse serviceResponse = authService.refreshAuthToken(refreshDto);

            // gRPC 응답으로 변환
            RefreshTokenResponse grpcResponse = RefreshTokenResponse.newBuilder()
                    .setAccessToken(serviceResponse.accessToken())
                    .setRefreshToken(serviceResponse.refreshToken())
                    .setExpiresIn(3600L) // 1시간
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (GrpcException e) {
            log.error("Invalid refresh token");
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("유효하지 않은 토큰입니다")
                    .asRuntimeException());
        } catch (IllegalAccessError e) {
            log.error("Refresh token expired");
            responseObserver.onError(Status.UNAUTHENTICATED
                    .withDescription("토큰이 만료되었습니다")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Refresh token failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("토큰 갱신 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> responseObserver) {
        try {
            log.info("Logout request received for userId: {}, deviceId: {}", request.getUserId(), request.getDeviceId());

            authService.logout(request.getUserId(), request.getDeviceId());

            // gRPC 응답
            LogoutResponse grpcResponse = LogoutResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("로그아웃이 완료되었습니다")
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (GrpcException e) {
            log.error("User not found for logout: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("사용자를 찾을 수 없습니다")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Logout failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("로그아웃 처리 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void withdraw(WithDrawRequest request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("withdraw request received for userId: {}", request.getUserId());

            authService.withdraw(request.getUserId());

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (GrpcException e) {
            log.error("User not found for logout: {}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("사용자를 찾을 수 없습니다")
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Logout failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("로그아웃 처리 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }
}