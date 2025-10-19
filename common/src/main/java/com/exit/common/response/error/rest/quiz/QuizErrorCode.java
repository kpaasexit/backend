package com.exit.common.response.error.rest.quiz;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuizErrorCode implements ErrorCode {
    GET_CATEGORY_STATISTICS_FAIL("QUIZ_ERR_002", HttpStatus.NOT_FOUND, "카테고리 별 통계 조회에 실패하였습니다."),
    GET_QUIZ_FAIL("QUIZ_ERR_003", HttpStatus.NOT_FOUND, "퀴즈 조회에 실패하였습니다."),
    SUBMIT_ANSWER_FAIL("QUIZ_ERR_004", HttpStatus.BAD_REQUEST, "답변 제출에 실패하였습니다."),
    GET_SOLVED_QUIZ_FAIL("QUIZ_ERR_005", HttpStatus.NOT_FOUND, "해결한 퀴즈 조회에 실패하였습니다."),
    GET_TODAY_QUIZ_FAIL("QUIZ_ERR_006", HttpStatus.BAD_REQUEST, "오늘의 퀴즈 조회에 실패하였습니다."),

    AVAILABLE_QUIZ_SERVER("QUIZ_ERR_099", HttpStatus.INTERNAL_SERVER_ERROR, "퀴즈 서버 다운!!!!");
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
