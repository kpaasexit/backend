package com.exit.notification.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcNotificationErrorCode implements GrpcErrorCode {
    FCM_SEND_FAILED(Status.Code.INTERNAL, "NOTIFICATION_ERR_001", "FCM 알림 전송 실패"),
    FCM_TOKEN_NOT_FOUND(Status.Code.NOT_FOUND, "NOTIFICATION_ERR_002", "fcm 토큰을 찾을 수 없음")
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
    private final String errorDescription;
}
