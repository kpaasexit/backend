package com.exit.common.response.error.rest.notification;

import com.exit.common.response.error.rest.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    FCM_SEND_FAILED("N001", HttpStatus.INTERNAL_SERVER_ERROR, "FCM 알림 전송에 실패했습니다"),
    INVALID_FCM_TOKEN("N002", HttpStatus.BAD_REQUEST, "유효하지 않은 FCM 토큰입니다"),
    FCM_TOKEN_NOT_FOUND("N003", HttpStatus.NOT_FOUND, "FCM 토큰을 찾을 수 없습니다"),
    NOTIFICATION_PERMISSION_DENIED("N004", HttpStatus.FORBIDDEN, "알림 권한이 없습니다"),
    NOTIFICATION_SETTINGS_NOT_FOUND("N005", HttpStatus.NOT_FOUND, "알림 설정을 찾을 수 없습니다"),
    FIREBASE_INITIALIZATION_FAILED("N006", HttpStatus.INTERNAL_SERVER_ERROR, "Firebase 초기화에 실패했습니다");

    private final String developCode;
    private final HttpStatus httpStatus;
    private final String errorDescription;
}