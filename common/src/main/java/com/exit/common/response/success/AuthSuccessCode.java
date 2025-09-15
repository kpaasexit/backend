package com.exit.common.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements SuccessCode {
    LOGIN_SUCCESS("AUTH_OK_001", HttpStatus.OK, "로그인 성공"),
    REISSUE_TOKEN_SUCCESS("AUTH_OK_002", HttpStatus.CREATED, "토큰 재발급 성공"),
    LOGOUT_SUCCESS("AUTH_OK_003", HttpStatus.OK, "로그아웃 성공");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
