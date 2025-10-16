package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcResponseErrorCode implements GrpcErrorCode {
    NULL_RESPONSE(Status.Code.NOT_FOUND, "RESPONSE_ERR_002", "답변이 존재하지 않음"),
    ALREADY_RESPONSE_ADOPTED(Status.Code.INVALID_ARGUMENT, "RESPONSE_ERR_003", "이미 채택된 답변입니다"),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
