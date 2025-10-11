package com.exit.common.response.success;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements SuccessCode {
    UPDATE_ADDITIONAL_INFO_SUCCESS("USER_OK_001", HttpStatus.OK, "추가 정보 업데이트 성공"),
    UPDATE_DEVICE_SUCCESS("USER_OK_002", HttpStatus.OK, "디바이스 업데이트 성공"),
    GET_USER_INFO_SUCCESS("USER_OK_003", HttpStatus.OK, "유저 정보 조회 성공"),
    ;

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
