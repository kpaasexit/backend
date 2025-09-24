package com.exit.gateway.controller.user;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.RefreshTokenResponse;
import com.exit.common.grpc.UpdateAdditionalUserInfoRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.UserErrorCode;
import com.exit.common.response.success.AuthSuccessCode;
import com.exit.common.response.success.UserSuccessCode;
import com.exit.gateway.controller.user.dto.request.auth.RefreshTokenRequestDto;
import com.exit.gateway.controller.user.dto.request.user.UpdateAdditionalUserInfoRequestDto;
import com.exit.gateway.controller.user.dto.response.auth.TokenResponseDto;
import com.exit.gateway.controller.user.dto.response.user.UpdateAdditionalUserInfoResponseDto;
import com.exit.gateway.service.user.UserGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserGrpcClient userGrpcClient;

    @PostMapping("/additional-info")
    public SuccessResponse<UpdateAdditionalUserInfoResponseDto> refresh(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateAdditionalUserInfoRequestDto request) {
        try {
            log.info("Update additional user info request received");
            UpdateAdditionalUserInfoResponse grpcResponse = userGrpcClient.updateAdditionalUserInfo(userId, request);

            return SuccessResponse.of(UserSuccessCode.UPDATE_ADDITIONAL_INFO_SUCCESS, UpdateAdditionalUserInfoResponseDto.from(grpcResponse));
        } catch (StatusRuntimeException e) {
            log.error("Update additional user info failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(UserErrorCode.UPDATE_ADDITIONAL_INFO_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Update additional user info failed", e);
            throw new RestApiException(UserErrorCode.UPDATE_ADDITIONAL_INFO_FAIL);
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
