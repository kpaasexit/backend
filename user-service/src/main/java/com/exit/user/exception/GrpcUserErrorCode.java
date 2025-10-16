package com.exit.user.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcUserErrorCode implements GrpcErrorCode {
    USER_NOT_FOUND(Status.Code.NOT_FOUND, "USER_ERR_004"),
    INVALID_REFRESH_TOKEN(Status.Code.UNAUTHENTICATED, "USER_ERR_005"),
    LOGOUT_FAILED(Status.Code.INTERNAL, "USER_ERR_006"),
    SOCIAL_LOGIN_FAILED(Status.Code.INTERNAL, "USER_ERR_007");

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
