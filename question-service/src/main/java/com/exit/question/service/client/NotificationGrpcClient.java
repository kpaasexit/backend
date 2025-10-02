package com.exit.question.service.client;

import com.exit.common.grpc.*;
import com.exit.question.controller.dto.request.SendNotificationRequestDto;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationGrpcClient {

    @GrpcClient("notification-service")
    private NotificationServiceGrpc.NotificationServiceBlockingStub notificationServiceStub;

    public SendNotificationResponse sendNotification(SendNotificationRequestDto requestDto) {
        try {
            log.debug("Sending notification via gRPC for userId: {}", requestDto.receiverId());
            return notificationServiceStub.sendNotification(requestDto.toSendNotificationRequest());
        } catch (StatusRuntimeException e) {
            log.error("gRPC send notification failed for userId {}: {}", requestDto.receiverId(), e.getStatus(), e);
            return null;
        }
    }
}