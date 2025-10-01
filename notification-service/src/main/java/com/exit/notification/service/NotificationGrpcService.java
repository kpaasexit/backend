package com.exit.notification.service;

import com.exit.common.grpc.NotificationServiceGrpc;
import com.exit.common.grpc.SendNotificationResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class NotificationGrpcService extends NotificationServiceGrpc.NotificationServiceImplBase {
    private final NotificationService notificationService;

    @Override
    public void sendNotification(com.exit.common.grpc.SendNotificationRequest request,
                                 StreamObserver<SendNotificationResponse> responseObserver) {
        try {
            log.info("Send Notification request received: {}", request.getBody());
            SendNotificationResponse response = notificationService.sendNotification(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Send Notification failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("알림 전송 중 오류가 발생했습니다.")
                    .asRuntimeException());
        }
    }
}


