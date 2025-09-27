package com.exit.common.response.error.rest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QuestionErrorCode implements ErrorCode {
    CREATE_ADDITIONAL_QUESTION_FAIL("QUESTION_ERR_001", HttpStatus.BAD_REQUEST, "추가 질문 생성에 실패하였습니다."),
    GET_ADDITIONAL_QUESTION_LIST_FAIL("QUESTION_ERR_002", HttpStatus.BAD_REQUEST, "추가 질문 리스트 조회에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
