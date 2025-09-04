package com.exit.user.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcUserErrorCode implements GrpcErrorCode {
    NULL_USER(Status.Code.UNAUTHENTICATED, "USER_ERR_001"),
    EXISTING_USER(Status.Code.UNAUTHENTICATED, "USER_ERR_002"),
    INVALID_TOKEN(Status.Code.UNAUTHENTICATED,"USER_ERR_003"),
    EXPIRED_TOKEN(Status.Code.FAILED_PRECONDITION, "USER_ERR_004"),
    INVALID_REDIRECT_URI(Status.Code.UNAUTHENTICATED, "USER_ERR_005");

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
