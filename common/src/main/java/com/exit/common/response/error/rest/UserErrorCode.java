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
    INVALID_REDIRECT_URI("AUTH_ERR_004", HttpStatus.BAD_REQUEST, "유효하지 않은 리다이렉트 경로입니다."),
    UNSUPPORTED_PROVIDER("AUTH_ERR_005", HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth2 제공자입니다."),
    UPDATE_ADDITIONAL_INFO_FAIL("AUTH_ERR_006", HttpStatus.BAD_REQUEST, "추가 정보 업데이트에 실패하였습니다."),
    UPDATE_DEVICE_FAIL("AUTH_ERR_007", HttpStatus.BAD_REQUEST, "디바이스 업데이트에 실패하였습니다."),
    REFRESH_FAIL("AUTH_ERR_008", HttpStatus.BAD_REQUEST, "토큰 재발급에 실패하였습니다."),
    LOGOUT_FAIL("AUTH_ERR_009", HttpStatus.BAD_REQUEST, "로그아웃에 실패하였습니다."),
    GET_USER_INFO_FAIL("AUTH_ERR_010", HttpStatus.BAD_REQUEST, "유저 정보 조회에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}
