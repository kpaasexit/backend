package com.exit.common.response.error.rest.search;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {
    SEARCH_FAIL("SEARCH_ERR_001", HttpStatus.BAD_REQUEST, "검색에 실패하였습니다."),
    SEARCH_KEYWORD_REQUIRED("SEARCH_ERR_002", HttpStatus.BAD_REQUEST, "검색 키워드는 필수입니다."),
    INVALID_SEARCH_TYPE("SEARCH_ERR_003", HttpStatus.BAD_REQUEST, "잘못된 검색 타입입니다."),
    SEARCH_SERVICE_UNAVAILABLE("SEARCH_ERR_004", HttpStatus.SERVICE_UNAVAILABLE, "검색 서비스를 일시적으로 사용할 수 없습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
