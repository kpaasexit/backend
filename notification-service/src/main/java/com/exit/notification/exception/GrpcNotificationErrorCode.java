package com.exit.notification.exception;

import com.exit.common.response.error.grpc.GrpcErrorCode;
import io.grpc.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GrpcNotificationErrorCode implements GrpcErrorCode {
    FCM_SEND_FAILED(Status.Code.INTERNAL, "NOTIFICATION_ERR_001"),
    FCM_TOKEN_NOT_FOUND(Status.Code.NOT_FOUND, "NOTIFICATION_ERR_002")
    ;

    private final Status.Code grpcStatusCode;
    private final String developCode;
}
