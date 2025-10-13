package com.exit.common.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchSuccessCode implements SuccessCode {
    SEARCH_SUCCESS("SEARCH_OK_001", HttpStatus.OK, "통합 검색 성공"),
    SEARCH_QUESTIONS_SUCCESS("SEARCH_OK_002", HttpStatus.OK, "질문 검색 성공"),
    SEARCH_MAGAZINES_SUCCESS("SEARCH_OK_003", HttpStatus.OK, "매거진 검색 성공"),
    ;

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
