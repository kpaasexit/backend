package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcQuestionErrorCode implements GrpcErrorCode {
    NULL_USER(Status.Code.UNAUTHENTICATED, "QUESTION_ERR_001"),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
