package com.exit.gateway.controller.user;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.UserErrorCode;
import com.exit.common.response.success.UserSuccessCode;
import com.exit.gateway.controller.user.dto.request.user.UpdateAdditionalUserInfoRequestDto;
import com.exit.gateway.controller.user.dto.response.user.CheckNicknameDuplicateResponseDto;
import com.exit.gateway.controller.user.dto.response.user.GetUserInfoResponseDto;
import com.exit.gateway.controller.user.dto.response.user.UpdateAdditionalUserInfoResponseDto;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.user.UserGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserGrpcClient userGrpcClient;

    @PutMapping(value = "/additional-info", consumes = "multipart/form-data")
    public SuccessResponse<UpdateAdditionalUserInfoResponseDto> updateAdditionalUserInfo(
            @LoginUser Long userId,
            @Valid @ModelAttribute UpdateAdditionalUserInfoRequestDto request) {
        try {
            log.info("Update additional user info request received");
            UpdateAdditionalUserInfoResponse grpcResponse = userGrpcClient.updateAdditionalUserInfo(userId, request);

            return SuccessResponse.of(UserSuccessCode.UPDATE_ADDITIONAL_INFO_SUCCESS,
                    UpdateAdditionalUserInfoResponseDto.from(grpcResponse));
        } catch (StatusRuntimeException e) {
            log.error("Update additional user info failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(UserErrorCode.UPDATE_ADDITIONAL_INFO_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Update additional user info failed", e);
            throw new RestApiException(UserErrorCode.UPDATE_ADDITIONAL_INFO_FAIL);
        }
    }


    @GetMapping("/info")
    public SuccessResponse<GetUserInfoResponseDto> getUserInfo(@LoginUser Long userId) {
        try {
            log.info("Get user info request received");
            return SuccessResponse.of(UserSuccessCode.GET_USER_INFO_SUCCESS,
                    userGrpcClient.getUserInfo(userId));
        } catch (StatusRuntimeException e) {
            log.error("Get user info failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(UserErrorCode.GET_USER_INFO_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Get user info failed", e);
            throw new RestApiException(UserErrorCode.GET_USER_INFO_FAIL);
        }
    }

    @GetMapping("/check-nickname")
    public SuccessResponse<CheckNicknameDuplicateResponseDto> checkNicknameDuplicate(
            @RequestParam String nickname) {
        try {
            log.info("Check nickname duplicate request received for nickname: {}", nickname);
            return SuccessResponse.of(UserSuccessCode.CHECK_NICKNAME_DUPLICATE_SUCCESS,
                    userGrpcClient.checkNicknameDuplicate(nickname));
        } catch (StatusRuntimeException e) {
            log.error("Check nickname duplicate failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(UserErrorCode.CHECK_NICKNAME_DUPLICATE_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Check nickname duplicate failed", e);
            throw new RestApiException(UserErrorCode.CHECK_NICKNAME_DUPLICATE_FAIL);
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
