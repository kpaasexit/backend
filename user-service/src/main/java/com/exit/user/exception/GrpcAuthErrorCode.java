package com.exit.user.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcAuthErrorCode implements GrpcErrorCode {
    // 토큰 관련
    INVALID_REFRESH_TOKEN(Status.Code.UNAUTHENTICATED, "AUTH_ERR_005", "유효하지 않은 리프레시 토큰"),
    REFRESH_TOKEN_FAILED(Status.Code.INTERNAL, "AUTH_ERR_015", "토큰 리프레시 실패"),

    // 작업 실패
    LOGOUT_FAILED(Status.Code.INTERNAL, "AUTH_ERR_006", "로그아웃 실패"),
    SOCIAL_LOGIN_FAILED(Status.Code.INTERNAL, "AUTH_ERR_007", "소셜로그인 실패"),
    WITHDRAW_FAILED(Status.Code.INTERNAL, "AUTH_ERR_012", "회원 탈퇴 실패"),
    USER_CREATION_FAILED(Status.Code.INTERNAL, "AUTH_ERR_013", "회원 생성 실패");

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}