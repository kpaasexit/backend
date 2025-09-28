package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcQuestionErrorCode implements GrpcErrorCode {
    NULL_RESPONSE(Status.Code.NOT_FOUND, "QUESTION_ERR_001"),
    NULL_QUESTION(Status.Code.NOT_FOUND, "QUESTION_ERR_002"),
    EXIST_ADOPTED_RESPONSE(Status.Code.ALREADY_EXISTS, "QUESTION_ERR_003"),
    NULL_ADDITIONAL_QUESTION(Status.Code.NOT_FOUND, "QUESTION_ERR_004"),
    UNAVAILABLE_COMMENT_TYPE(Status.Code.UNAVAILABLE, "QUESTION_ERR_005");


    private final Status.Code grpcStatusCode;
    private final String developCode;
}
