package com.exit.quiz.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcQuizErrorCode implements GrpcErrorCode {
    NO_AVAILABLE_QUIZ(Status.Code.UNAVAILABLE, "QUIZ_ERR_001", "풀 수 있는 퀴즈가 없습니다."),
    NOT_FOUND_QUIZ(Status.Code.NOT_FOUND, "QUIZ_ERR_002", "요청 퀴즈가 존재하지 않습니다."),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
