package com.exit.gateway.controller.user;

import com.exit.common.auth.jwt.JwtTokenProvider;
import com.exit.common.grpc.RefreshTokenResponse;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.AuthSuccessCode;
import com.exit.common.response.success.UserSuccessCode;
import com.exit.gateway.controller.user.dto.request.auth.DeviceFcmTokenRequestDto;
import com.exit.gateway.controller.user.dto.request.auth.RefreshTokenRequestDto;
import com.exit.gateway.controller.user.dto.response.auth.TokenResponseDto;
import com.exit.gateway.global.annotation.DeviceId;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.user.AuthGrpcClient;
import com.exit.gateway.service.user.UserGrpcClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthGrpcClient authGrpcClient;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/refresh")
    public SuccessResponse<TokenResponseDto> refresh(
            @CookieValue("refreshToken") String refreshToken) {
        log.info("Token refresh request received");
        String deviceId = jwtTokenProvider.getDeviceIdFromRefreshToken(refreshToken);
        log.debug("Refresh token received : {}", deviceId);
        RefreshTokenResponse grpcResponse = authGrpcClient.refreshToken(refreshToken, deviceId);
        TokenResponseDto response = new TokenResponseDto(
                grpcResponse.getAccessToken(),
                grpcResponse.getRefreshToken()
        );

        return SuccessResponse.of(AuthSuccessCode.LOGIN_SUCCESS, response);
    }

    @PostMapping("/logout")
    public SuccessResponse<String> logout(
            @LoginUser Long userId,
            @DeviceId String deviceId
    ) {
        log.info("Logout request received for userId: {}", userId);
        authGrpcClient.logout(userId, deviceId);
        return SuccessResponse.of(AuthSuccessCode.LOGIN_SUCCESS,
                "로그아웃이 완료되었습니다.");
    }

    @PostMapping("/withdraw")
    public SuccessResponse<String> withdraw(@LoginUser Long userId) {
        log.info("withdraw request received for userId: {}", userId);
        authGrpcClient.withdraw(userId);

        return SuccessResponse.of(AuthSuccessCode.DELETE_USER_SUCCESS,
                "회원탈퇴가 완료되었습니다.");
    }
}