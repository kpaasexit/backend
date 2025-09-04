package com.exit.common.response.error.rest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    NULL_USER("AUTH_ERR_001", HttpStatus.BAD_REQUEST, "사용자가 존재하지 않습니다."),
    INVALID_TOKEN("AUTH_ERR_002", HttpStatus.BAD_REQUEST, "올바르지 않은 토큰입니다."),
    EXPIRED_TOKEN("AUTH_ERR_003", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_REDIRECT_URI("AUTH_ERR_004", HttpStatus.BAD_REQUEST, "유효하지 않은 리다이렉트 경로입니다.");

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
