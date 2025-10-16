package com.exit.user.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcAuthErrorCode implements GrpcErrorCode {
    // 토큰 관련
    INVALID_REFRESH_TOKEN(Status.Code.UNAUTHENTICATED, "AUTH_ERR_005"),
    REFRESH_TOKEN_FAILED(Status.Code.INTERNAL, "AUTH_ERR_015"),

    // 작업 실패
    LOGOUT_FAILED(Status.Code.INTERNAL, "AUTH_ERR_006"),
    SOCIAL_LOGIN_FAILED(Status.Code.INTERNAL, "AUTH_ERR_007"),
    WITHDRAW_FAILED(Status.Code.INTERNAL, "AUTH_ERR_012"),
    USER_CREATION_FAILED(Status.Code.INTERNAL, "AUTH_ERR_013");

    private final Status.Code grpcStatusCode;
    private final String developCode;
}