package com.exit.common.response.error.rest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ValidationErrorCode implements ErrorCode {
    VALIDATION_FAILED("VALID_ERR_001", HttpStatus.BAD_REQUEST, "유효성 검증에 실패하였습니다.");

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
