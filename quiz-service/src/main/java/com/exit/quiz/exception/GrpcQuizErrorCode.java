package com.exit.quiz.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcQuizErrorCode implements GrpcErrorCode {
    // 퀴즈 관련
    NO_AVAILABLE_QUIZ(Status.Code.UNAVAILABLE, "QUIZ_ERR_001", "풀 수 있는 퀴즈가 없습니다."),
    NOT_FOUND_QUIZ(Status.Code.NOT_FOUND, "QUIZ_ERR_002", "요청 퀴즈가 존재하지 않습니다."),

    // 카테고리 통계 조회 실패
    GET_CATEGORY_STATISTICS_FAILED(Status.Code.INTERNAL, "QUIZ_ERR_020", "카테고리 통계 조회 실패"),

    // 퀴즈 조회 실패
    GET_QUIZ_FAILED(Status.Code.INTERNAL, "QUIZ_ERR_021", "퀴즈 조회 실패"),
    GET_TODAY_QUIZ_FAILED(Status.Code.INTERNAL, "QUIZ_ERR_022", "오늘의 퀴즈 조회 실패"),
    GET_SOLVED_QUIZ_FAILED(Status.Code.INTERNAL, "QUIZ_ERR_023", "풀었던 퀴즈 목록 조회 실패"),

    // 퀴즈 제출 실패
    SUBMIT_ANSWER_FAILED(Status.Code.INTERNAL, "QUIZ_ERR_030", "퀴즈 답안 제출 실패"),
    SAVE_QUIZ_ATTEMPT_FAILED(Status.Code.INTERNAL, "QUIZ_ERR_031", "퀴즈 시도 기록 저장 실패"),

    // 입력 검증 실패
    INVALID_CATEGORY_ID(Status.Code.INVALID_ARGUMENT, "QUIZ_ERR_040", "유효하지 않은 카테고리 ID"),
    INVALID_USER_ID(Status.Code.INVALID_ARGUMENT, "QUIZ_ERR_042", "유효하지 않은 사용자 ID"),
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
