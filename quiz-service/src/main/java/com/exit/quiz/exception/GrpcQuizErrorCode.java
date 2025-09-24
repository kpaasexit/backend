package com.exit.quiz.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcQuizErrorCode implements GrpcErrorCode {
    NO_AVAILABLE_QUIZ(Status.Code.UNAVAILABLE, "QUIZ_ERR_001"),
    NOT_FOUND_QUIZ(Status.Code.NOT_FOUND, "QUIZ_ERR_002"),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
