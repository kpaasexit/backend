package com.exit.common.response.error.rest.user;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    NULL_USER("USER_ERR_001", HttpStatus.BAD_REQUEST, "사용자가 존재하지 않습니다."),

    UPDATE_ADDITIONAL_INFO_FAIL("USER_ERR_006", HttpStatus.BAD_REQUEST, "추가 정보 업데이트에 실패하였습니다."),
    GET_USER_INFO_FAIL("USER_ERR_010", HttpStatus.BAD_REQUEST, "유저 정보 조회에 실패하였습니다."),

    UPDATE_DEVICE_FAIL("USER_ERR_007", HttpStatus.BAD_REQUEST, "디바이스 업데이트에 실패하였습니다."),
    CHECK_NICKNAME_DUPLICATE_FAIL("USER_ERR_012", HttpStatus.BAD_REQUEST, "닉네임 중복 체크에 실패하였습니다."),
    AVAILABLE_USER_SERVER("USER_ERR_099", HttpStatus.INTERNAL_SERVER_ERROR, "유저 서버 다운!!!!"),
    ;

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}