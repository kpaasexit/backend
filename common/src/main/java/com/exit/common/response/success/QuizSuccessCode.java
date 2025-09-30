package com.exit.common.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuizSuccessCode implements SuccessCode {
    GET_CATEGORY_STATISTICS_SUCCESS("QUIZ_OK_001", HttpStatus.CREATED, "카테고리 통계 조사 성공"),
    GET_QUIZ_SUCCESS("QUIZ_OK_002", HttpStatus.OK, "퀴즈 조회 성공"),
    SUBMIT_ANSWER_SUCCESS("QUIZ_OK_003", HttpStatus.OK, "답안 제출 성공"),
    REPORT_QUIZ_SUCCESS("QUIZ_OK_004", HttpStatus.OK, "퀴즈 신고 성공"),
    GET_SOLVED_QUIZ_SUCCESS("QUIZ_OK_005", HttpStatus.OK, "도전한 퀴즈 내역 조회 성공"),
    RESOLVE_QUIZ_SUCCESS("QUIZ_OK_006", HttpStatus.OK, "재도전 퀴즈 조회 성공");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
