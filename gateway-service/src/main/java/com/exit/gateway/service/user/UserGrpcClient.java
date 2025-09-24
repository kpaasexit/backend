package com.exit.gateway.service.user;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGrpcClient {

    @GrpcClient("user-service")
    private SocialAuthServiceGrpc.SocialAuthServiceBlockingStub socialAuthServiceStub;

    public SocialLoginResponse socialLogin(OAuth2UserInfo userInfo) {
        try {
            // OAuth2UserInfo를 gRPC SocialLoginWithUserInfoRequest로 변환
            SocialLoginRequest request = SocialLoginRequest.newBuilder()
                    .setProvider(userInfo.getProvider())
                    .setSocialId(userInfo.getProviderId())
                    .setEmail(userInfo.getEmail() != null ? userInfo.getEmail() : "")
                    .setName(userInfo.getName() != null ? userInfo.getName() : "")
                    .build();

            log.debug("Sending social login with user info via gRPC: {}", userInfo.getProvider());
            SocialLoginResponse response = socialAuthServiceStub.socialLogin(request);
            log.debug("Received social login response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC social login with user info failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public RefreshTokenResponse refreshToken(String refreshToken) {
        try {
            RefreshTokenRequest request = RefreshTokenRequest.newBuilder()
                    .setRefreshToken(refreshToken)
                    .build();

            log.debug("Sending refresh token request via gRPC: {}", request);
            RefreshTokenResponse response = socialAuthServiceStub.refreshToken(request);
            log.debug("Received refresh token response via gRPC: {}", response);

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC refresh token failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public LogoutResponse logout(Long userId) {
        try {
            LogoutRequest request = LogoutRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            log.debug("Sending logout request via gRPC: {}", request);
            LogoutResponse response = socialAuthServiceStub.logout(request);
            log.debug("Received logout response via gRPC: {}", response);

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC logout failed: {}", e.getStatus(), e);
            throw e;
        }
    }
}