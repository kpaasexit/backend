package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcQuestionErrorCode implements GrpcErrorCode {
    NOT_FOUND_QUESTION(Status.Code.NOT_FOUND, "QUESTION_ERR_002", "질문을 찾을 수 없습니다."),
    EXIST_ADOPTED_RESPONSE(Status.Code.ALREADY_EXISTS, "QUESTION_ERR_003", "이미 채택된 답변이 있습니다."),
    NULL_ADDITIONAL_QUESTION(Status.Code.NOT_FOUND, "QUESTION_ERR_004", "추가 질문이 없습니다."),
    UNAVAILABLE_COMMENT_TYPE(Status.Code.UNAVAILABLE, "QUESTION_ERR_005", "지원하지 않는 댓글 타입입니다."),
    NULL_QUESTION_CATEGORY(Status.Code.NOT_FOUND, "QUESTION_ERR_006", "해당 카테고리가 존재하지 않습니다."),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}