package com.exit.gateway.service.user;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.user.dto.request.user.UpdateAdditionalUserInfoRequestDto;
import com.exit.gateway.controller.user.dto.response.auth.oauth2.OAuth2UserInfo;
import com.exit.gateway.service.user.util.UserGrpcMapper;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserGrpcClient {
    private final UserGrpcMapper userGrpcMapper;
    @GrpcClient("user-service")
    private SocialAuthServiceGrpc.SocialAuthServiceBlockingStub socialAuthServiceStub;
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    public SocialLoginResponse socialLogin(OAuth2UserInfo userInfo, String deviceId) {
        try {
            SocialLoginRequest request = SocialLoginRequest.newBuilder()
                    .setProvider(userInfo.getProvider())
                    .setSocialId(userInfo.getProviderId())
                    .setEmail(userInfo.getEmail() != null ? userInfo.getEmail() : "")
                    .setName(userInfo.getName() != null ? userInfo.getName() : "")
                    .setDeviceId(deviceId)
                    .build();

            log.debug("Sending social login via gRPC - provider: {}, deviceId: {}",
                    userInfo.getProvider(), deviceId);
            SocialLoginResponse response = socialAuthServiceStub.socialLogin(request);
            log.debug("Received social login response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC social login failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public RefreshTokenResponse refreshToken(String refreshToken, String deviceId) {
        try {
            RefreshTokenRequest request = RefreshTokenRequest.newBuilder()
                    .setRefreshToken(refreshToken)
                    .setDeviceId(deviceId)
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

    public LogoutResponse logout(Long userId, String deviceId) {
        try {
            LogoutRequest request = LogoutRequest.newBuilder()
                    .setUserId(userId)
                    .setDeviceId(deviceId)
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

    public UpdateAdditionalUserInfoResponse updateAdditionalUserInfo(Long userId, UpdateAdditionalUserInfoRequestDto requestDto) throws IOException {
        try {
            MultipartFile image = requestDto.image();
            UpdateAdditionalUserInfoRequest request;
            if (image != null) {
                ImageMetadata metaData = ImageMetadata.newBuilder()
                        .setFilename(image.getOriginalFilename())
                        .setContentType(image.getContentType())
                        .build();
                UploadBytesRequest uploadBytesRequest = UploadBytesRequest.newBuilder()
                        .setMeta(metaData)
                        .setData(ByteString.copyFrom(image.getBytes()))
                        .build();
                request = UpdateAdditionalUserInfoRequest.newBuilder()
                        .setUserId(userId)
                        .setUserName(requestDto.nickname())
                        .setImageFile(uploadBytesRequest)
                        .build();
            } else {
                request = UpdateAdditionalUserInfoRequest.newBuilder()
                        .setUserId(userId)
                        .setUserName(requestDto.nickname())
                        .build();
            }

            log.debug("Sending updateAdditionalUserInfo request via gRPC: {}", request);
            UpdateAdditionalUserInfoResponse response = userServiceStub.updateAdditionalUserInfo(request);
            log.debug("Received updateAdditionalUserInfo response via gRPC: {}", response);

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC updateAdditionalUserInfo failed: {}", e.getStatus(), e);
            throw e;
        } catch (IOException e) {
            log.error("image bytes data extraction failed");
            throw e;
        }
    }
}