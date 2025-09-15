package com.exit.gateway.controller;

import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.AuthSuccessCode;
import com.exit.gateway.controller.dto.request.LoginRequestDto;
import com.exit.gateway.controller.dto.request.RefreshTokenRequestDto;
import com.exit.gateway.controller.dto.response.AuthResponseDto;
import com.exit.gateway.controller.dto.response.TokenResponseDto;
import com.exit.gateway.service.UserGrpcClient;
import com.exit.common.grpc.SocialLoginResponse;
import com.exit.common.grpc.RefreshTokenResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserGrpcClient userGrpcClient;

    @PostMapping("/refresh")
    public SuccessResponse<TokenResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        try {
            log.info("Token refresh request received");
            RefreshTokenResponse grpcResponse = userGrpcClient.refreshToken(request.getRefreshToken());
            
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
    public SuccessResponse<String> logout(@AuthenticationPrincipal Long userId) {
        try {
            log.info("Logout request received for userId: {}", userId);
            userGrpcClient.logout(userId);
            
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

    private Long getUserIdFromRequest(HttpServletRequest request) {
        // JWT 토큰에서 사용자 ID를 추출하는 로직
        // 실제 구현에서는 JWT 파싱 로직이 필요합니다.
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // JWT 파싱 로직을 통해 userId 추출
            // 임시로 1L을 반환 (실제로는 JWT에서 추출해야 함)
            return 1L;
        }
        throw new RuntimeException("유효하지 않은 토큰입니다.");
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