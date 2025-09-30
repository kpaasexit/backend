package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcResponseErrorCode implements GrpcErrorCode {
    EXIST_RESPONSE_LIKE(Status.Code.UNAVAILABLE, "RESPONSE_ERR_001"),
    NULL_RESPONSE(Status.Code.NOT_FOUND, "RESPONSE_ERR_002"),
    ALREADY_RESPONSE_ADOPTED(Status.Code.INVALID_ARGUMENT, "RESPONSE_ERR_003"),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
