package com.exit.notification.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.notification.controller.dto.NotificationRequest;
import com.exit.notification.controller.dto.NotificationResponse;
import com.exit.notification.exception.GrpcNotificationErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final FirebaseMessaging firebaseMessaging;

    public NotificationResponse sendNotification(NotificationRequest request) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(request.getTitle())
                    .setBody(request.getBody())
                    .build();

            Message message = Message.builder()
                    .setToken(request.getToken())
                    .setNotification(notification)
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Successfully sent notification: {}", response);

            return NotificationResponse.builder()
                    .messageId(response)
                    .success(true)
                    .sentAt(LocalDateTime.now())
                    .build();

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token: {}", request.getToken(), e);
            throw new GrpcException(GrpcNotificationErrorCode.FCM_SEND_FAILED);
        }
    }

    public void sendNotificationAsync(NotificationRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                sendNotification(request);
            } catch (Exception e) {
                log.error("Async notification sending failed", e);
            }
        });
    }

    public void sendMultipleNotifications(List<NotificationRequest> requests) {
        requests.forEach(this::sendNotificationAsync);
    }

    public boolean validateToken(String token) {
        try {
            Message testMessage = Message.builder()
                    .setToken(token)
                    .putData("test", "validation")
                    .build();

            firebaseMessaging.send(testMessage, true);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("Invalid FCM token: {}", token);
            return false;
        }
    }
}
