package com.exit.gateway.controller.user;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.RefreshTokenResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.UserErrorCode;
import com.exit.common.response.success.AuthSuccessCode;
import com.exit.common.response.success.UserSuccessCode;
import com.exit.gateway.controller.user.dto.request.auth.DeviceFcmTokenRequestDto;
import com.exit.gateway.controller.user.dto.request.auth.RefreshTokenRequestDto;
import com.exit.gateway.controller.user.dto.response.auth.TokenResponseDto;
import com.exit.gateway.global.annotation.DeviceId;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.user.UserGrpcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @PostMapping("/device")
    public SuccessResponse<String> updateDevice(
            @LoginUser Long userId,
            @DeviceId String deviceId,
            @Valid @RequestBody DeviceFcmTokenRequestDto request) {
        try {
            log.info("deviceId and fcmToken request received");
            userGrpcClient.updateDevice(userId, deviceId, request.fcmToken());

            return SuccessResponse.of(UserSuccessCode.UPDATE_DEVICE_SUCCESS, "디바이스 정보 업데이트를 완료하였습니다.");
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
            @Valid @RequestBody RefreshTokenRequestDto request) {
        try {
            log.info("Token refresh request received");
            String deviceId = jwtTokenProvider.getDeviceIdFromToken(request.getRefreshToken());
            log.debug("Refresh token received : {}", deviceId);
            RefreshTokenResponse grpcResponse = userGrpcClient.refreshToken(request.getRefreshToken(), deviceId);
            TokenResponseDto response = new TokenResponseDto(
                    grpcResponse.getAccessToken(),
                    grpcResponse.getRefreshToken()
            );

            return SuccessResponse.of(AuthSuccessCode.LOGIN_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Token refresh failed via gRPC: {}", e.getStatus(), e);
            String developCode = extractDevelopCodeFromGrpcError(e);

            // INVALID_REFRESH_TOKEN (USER_ERR_005)인 경우 EXPIRED_REFRESH_TOKEN으로 변환
            if ("USER_ERR_005".equals(developCode)) {
                throw new RestApiException(UserErrorCode.EXPIRED_REFRESH_TOKEN);
            }

            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(UserErrorCode.REFRESH_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new RestApiException(UserErrorCode.REFRESH_FAIL, e.getMessage());
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
            throw new RestApiException(UserErrorCode.LOGOUT_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw new RestApiException(UserErrorCode.LOGOUT_FAIL, e.getMessage());
        }
    }

    @PostMapping("/withdraw")
    public SuccessResponse<String> withdraw(@LoginUser Long userId) {
        try {
            log.info("withdraw request received for userId: {}", userId);
            userGrpcClient.withdraw(userId);

            return SuccessResponse.of(AuthSuccessCode.DELETE_USER_SUCCESS, "회원탈퇴가 완료되었습니다.");
        } catch (StatusRuntimeException e) {
            log.error("Logout failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(UserErrorCode.DELETED_USER_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw new RestApiException(UserErrorCode.DELETED_USER_FAIL, e.getMessage());
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

    /**
     * gRPC 에러로부터 developCode를 추출합니다.
     * GrpcExceptionResponseBody가 JSON으로 직렬화되어 description에 포함되어 있습니다.
     */
    private String extractDevelopCodeFromGrpcError(StatusRuntimeException e) {
        try {
            String description = e.getStatus().getDescription();
            if (description == null || description.isEmpty()) {
                return null;
            }

            // JSON 파싱 시도
            var errorBody = objectMapper.readTree(description);
            return errorBody.has("developCode") ? errorBody.get("developCode").asText() : null;
        } catch (Exception ex) {
            log.debug("Failed to parse gRPC error description: {}", ex.getMessage());
            return null;
        }
    }
}