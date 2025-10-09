package com.exit.gateway.controller.user;

import com.exit.common.grpc.RefreshTokenResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.AuthSuccessCode;
import com.exit.gateway.controller.user.dto.request.auth.DeviceFcmTokenRequestDto;
import com.exit.gateway.controller.user.dto.request.auth.RefreshTokenRequestDto;
import com.exit.gateway.controller.user.dto.response.auth.TokenResponseDto;
import com.exit.gateway.global.annotation.DeviceId;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.user.UserGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserGrpcClient userGrpcClient;

    @PostMapping("/device")
    public SuccessResponse<Void> updateDevice(
            @LoginUser Long userId,
            @DeviceId String deviceId,
            @Valid @RequestBody DeviceFcmTokenRequestDto request) {
        try {
            log.info("deviceId and fcmToken request received");
            userGrpcClient.updateDevice(userId, deviceId, request.fcmToken());

            return SuccessResponse.of(AuthSuccessCode.UPDATE_DEVICE_SUCCESS, null);
        } catch (StatusRuntimeException e) {
            log.error("Update Device failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(UserErrorCode.UPDATE_DEVICE_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Update Device failed", e);
            throw new RestApiException(UserErrorCode.UPDATE_DEVICE_FAIL, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public SuccessResponse<TokenResponseDto> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request,
            @DeviceId String deviceId) {
        try {
            log.info("Token refresh request received");
            RefreshTokenResponse grpcResponse = userGrpcClient.refreshToken(request.getRefreshToken(), deviceId);

            TokenResponseDto response = new TokenResponseDto(
                    grpcResponse.getAccessToken(),
                    grpcResponse.getRefreshToken()
            );

            return SuccessResponse.of(AuthSuccessCode.LOGIN_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Token refresh failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RuntimeException(errorMessage);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new RuntimeException("토큰 갱신에 실패했습니다.");
        }
    }

    @PostMapping("/logout")
    public SuccessResponse<String> logout(
            @LoginUser Long userId,
            @DeviceId String deviceId
    ) {
        try {
            log.info("Logout request received for userId: {}", userId);
            userGrpcClient.logout(userId, deviceId);

            return SuccessResponse.of(AuthSuccessCode.LOGIN_SUCCESS, "로그아웃이 완료되었습니다.");
        } catch (StatusRuntimeException e) {
            log.error("Logout failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RuntimeException(errorMessage);
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw new RuntimeException("로그아웃에 실패했습니다.");
        }
    }

    private String getGrpcErrorMessage(StatusRuntimeException e) {
        Status status = e.getStatus();
        switch (status.getCode()) {
            case INVALID_ARGUMENT:
                return "잘못된 요청입니다.";
            case UNAUTHENTICATED:
                return "인증에 실패했습니다.";
            case PERMISSION_DENIED:
                return "권한이 없습니다.";
            case NOT_FOUND:
                return "사용자를 찾을 수 없습니다.";
            default:
                return status.getDescription() != null ? status.getDescription() : "서버 오류가 발생했습니다.";
        }
    }
}