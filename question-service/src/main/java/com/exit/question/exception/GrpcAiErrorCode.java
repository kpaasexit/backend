package com.exit.question.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcAiErrorCode implements GrpcErrorCode {
    AI_ANSWER_FAIL(Status.Code.INTERNAL, "AI_ERR_001", "AI 답변 생성 중 오류가 발생했습니다."),
    AI_CATEGORY_RECOMMEND_FAIL(Status.Code.INTERNAL, "AI_ERR_002", "AI 카테고리 추천 중 오류가 발생했습니다."),
    AI_SIMILAR_QUESTION_FAIL(Status.Code.INTERNAL, "AI_ERR_003", "AI 유사 질문 찾기 중 오류가 발생했습니다."),
    AI_SAVE_QUESTION_FAIL(Status.Code.INTERNAL, "AI_ERR_004", "백터 디비 질문 저장 중 오류가 발생했습니다."),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
