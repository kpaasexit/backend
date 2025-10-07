package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcAiErrorCode implements GrpcErrorCode {
    AI_ANSWER_FAIL(Status.Code.INTERNAL, "AI_ERR_001"),
    AI_CATEGORY_RECOMMEND_FAIL(Status.Code.INTERNAL, "AI_ERR_002"),
    AI_SIMILAR_QUESTION_FAIL(Status.Code.INTERNAL, "AI_ERR_003"),
    AI_SAVE_QUESTION_FAIL(Status.Code.INTERNAL, "AI_ERR_004"),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
