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
import com.exit.gateway.global.util.GrpcExceptionConverter;
import com.exit.gateway.service.user.UserGrpcClient;
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
    private final GrpcExceptionConverter grpcExceptionConverter;

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
            throw grpcExceptionConverter.convert(e, UserErrorCode.UPDATE_DEVICE_FAIL);
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
            String deviceId = jwtTokenProvider.getDeviceIdFromRefreshToken(request.getRefreshToken());
            log.debug("Refresh token received : {}", deviceId);
            RefreshTokenResponse grpcResponse = userGrpcClient.refreshToken(request.getRefreshToken(), deviceId);
            TokenResponseDto response = new TokenResponseDto(
                    grpcResponse.getAccessToken(),
                    grpcResponse.getRefreshToken()
            );

            return SuccessResponse.of(AuthSuccessCode.LOGIN_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Token refresh failed via gRPC: {}", e.getStatus(), e);
            throw grpcExceptionConverter.convert(e, UserErrorCode.REFRESH_FAIL);
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
            throw grpcExceptionConverter.convert(e, UserErrorCode.LOGOUT_FAIL);
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
            log.error("Withdraw failed via gRPC: {}", e.getStatus(), e);
            throw grpcExceptionConverter.convert(e, UserErrorCode.DELETED_USER_FAIL);
        } catch (Exception e) {
            log.error("Withdraw failed", e);
            throw new RestApiException(UserErrorCode.DELETED_USER_FAIL, e.getMessage());
        }
    }
}