package com.exit.gateway.controller.user;

import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.UserSuccessCode;
import com.exit.gateway.controller.user.dto.request.auth.DeviceFcmTokenRequestDto;
import com.exit.gateway.controller.user.dto.request.user.UpdateAdditionalUserInfoRequestDto;
import com.exit.gateway.controller.user.dto.response.user.CheckNicknameDuplicateResponseDto;
import com.exit.gateway.controller.user.dto.response.user.GetUserInfoResponseDto;
import com.exit.gateway.controller.user.dto.response.user.UpdateAdditionalUserInfoResponseDto;
import com.exit.gateway.global.annotation.DeviceId;
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

    @PostMapping("/device")
    public SuccessResponse<String> updateDevice(
            @LoginUser Long userId,
            @DeviceId String deviceId,
            @Valid @RequestBody DeviceFcmTokenRequestDto request) {
        log.info("deviceId and fcmToken request received");
        userGrpcClient.updateDevice(userId, deviceId, request.fcmToken());

        return SuccessResponse.of(UserSuccessCode.UPDATE_DEVICE_SUCCESS,
                "디바이스 정보 업데이트를 완료하였습니다.");
    }

    @PutMapping(value = "/additional-info", consumes = "multipart/form-data")
    public SuccessResponse<UpdateAdditionalUserInfoResponseDto> updateAdditionalUserInfo(
            @LoginUser Long userId,
            @Valid @ModelAttribute UpdateAdditionalUserInfoRequestDto request) {
        log.info("Update additional user info request received");
        UpdateAdditionalUserInfoResponse grpcResponse = userGrpcClient.updateAdditionalUserInfo(userId, request);

        return SuccessResponse.of(UserSuccessCode.UPDATE_ADDITIONAL_INFO_SUCCESS,
                UpdateAdditionalUserInfoResponseDto.from(grpcResponse));
    }

    @GetMapping("/info")
    public SuccessResponse<GetUserInfoResponseDto> getUserInfo(@LoginUser Long userId) {
        log.info("Get user info request received");
        return SuccessResponse.of(UserSuccessCode.GET_USER_INFO_SUCCESS,
                userGrpcClient.getUserInfo(userId));
    }

    @GetMapping("/check-nickname")
    public SuccessResponse<CheckNicknameDuplicateResponseDto> checkNicknameDuplicate(
            @RequestParam String nickname) {
        log.info("Check nickname duplicate request received for nickname: {}", nickname);
        return SuccessResponse.of(UserSuccessCode.CHECK_NICKNAME_DUPLICATE_SUCCESS,
                userGrpcClient.checkNicknameDuplicate(nickname));
    }
}