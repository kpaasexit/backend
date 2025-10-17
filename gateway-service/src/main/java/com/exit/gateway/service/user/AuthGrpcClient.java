package com.exit.gateway.service.user;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.user.AuthGrpcErrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@GrpcToRest(mapper = AuthGrpcErrorMapper.class)
public class AuthGrpcClient {

    @GrpcClient("user-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authServiceStub;

    public SocialLoginResponse socialLogin(OAuth2UserInfo userInfo, String deviceId) {
        SocialLoginRequest request = SocialLoginRequest.newBuilder()
                .setProvider(userInfo.getProvider())
                .setSocialId(userInfo.getProviderId())
                .setEmail(userInfo.getEmail() != null ? userInfo.getEmail() : "")
                .setName(userInfo.getName() != null ? userInfo.getName() : "")
                .setDeviceId(deviceId)
                .build();

        log.debug("Sending social login via gRPC - provider: {}, deviceId: {}",
                userInfo.getProvider(), deviceId);
        SocialLoginResponse response = authServiceStub.socialLogin(request);
        log.debug("Received social login response via gRPC");

        return response;
    }

    public RefreshTokenResponse refreshToken(String refreshToken, String deviceId) {
        RefreshTokenRequest request = RefreshTokenRequest.newBuilder()
                .setRefreshToken(refreshToken)
                .setDeviceId(deviceId)
                .build();

        log.debug("Sending refresh token request via gRPC: {}", request);
        RefreshTokenResponse response = authServiceStub.refreshToken(request);
        log.debug("Received refresh token response via gRPC: {}", response);

        return response;
    }

    public LogoutResponse logout(Long userId, String deviceId) {
        LogoutRequest request = LogoutRequest.newBuilder()
                .setUserId(userId)
                .setDeviceId(deviceId)
                .build();

        log.debug("Sending logout request via gRPC: {}", request);
        LogoutResponse response = authServiceStub.logout(request);
        log.debug("Received logout response via gRPC: {}", response);

        return response;
    }

    public void withdraw(Long userId) {
        WithDrawRequest request = WithDrawRequest.newBuilder()
                .setUserId(userId)
                .build();

        log.debug("Withdraw user request via gRPC: {}", request);
        authServiceStub.withdraw(request);
    }
}