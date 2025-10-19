package com.exit.common.response.error.rest.user;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    INVALID_TOKEN("AUTH_ERR_002", HttpStatus.BAD_REQUEST, "올바르지 않은 토큰입니다."),
    EXPIRED_TOKEN("AUTH_ERR_003", HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    INVALID_REFRESH_TOKEN("AUTH_ERR_013", HttpStatus.BAD_REQUEST, "올바르지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN("AUTH_ERR_014", HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    SOCIAL_LOGIN_FAIL("AUTH_ERR_015", HttpStatus.BAD_REQUEST, "소셜 로그인에 실패하였습니다."),

    REFRESH_FAIL("AUTH_ERR_008", HttpStatus.BAD_REQUEST, "토큰 재발급에 실패하였습니다."),
    LOGOUT_FAIL("AUTH_ERR_009", HttpStatus.BAD_REQUEST, "로그아웃에 실패하였습니다."),
    DELETED_USER_FAIL("AUTH_ERR_011", HttpStatus.BAD_REQUEST, "회원 탈퇴에 실패하였습니다."),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}