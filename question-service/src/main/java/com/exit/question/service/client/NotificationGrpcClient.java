package com.exit.question.service.client;

import com.exit.common.grpc.NotificationServiceGrpc;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.common.grpc.SendNotificationResponse;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationGrpcClient {

    @GrpcClient("notification-service")
    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceStub;

    public SendNotificationResponse sendNotification(SendNotificationRequest request) {
        try {
            log.debug("Sending notification via gRPC for userId: {}", request.getReceiverId());
            return notificationServiceStub.sendNotification(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC send notification failed for userId {}: {}", request.getReceiverId(), e.getStatus(), e);
            return null;
        }
    }
}