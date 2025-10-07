package com.exit.notification.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.common.grpc.SendNotificationResponse;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.notification.domain.Notification;
import com.exit.notification.domain.NotificationType;
import com.exit.notification.domain.repository.NotificationRepository;
import com.exit.notification.exception.GrpcNotificationErrorCode;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final FirebaseMessaging firebaseMessaging;
    private final UserGrpcClient userGrpcClient;
    private final NotificationRepository notificationRepository;

    public SendNotificationResponse sendNotification(SendNotificationRequest request) {
        try {
            com.google.firebase.messaging.Notification notification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(NotificationType.valueOf(request.getType()).getTitle())
                    .setBody(request.getBody())
                    .build();

            List<String> fcmTokens = userGrpcClient.getFcmToken(request.getReceiverId());

            if (fcmTokens == null || fcmTokens.isEmpty()) {
                log.warn("No FCM tokens found for user: {}", request.getReceiverId());
                throw new GrpcException(GrpcNotificationErrorCode.FCM_TOKEN_NOT_FOUND);
            }

            List<Message> messageList = new ArrayList<>();
            fcmTokens.forEach(fcmToken -> {
                Message message = Message.builder()
                        .setToken(fcmToken)
                        .setNotification(notification)
                        .build();

                messageList.add(message);
            });

            BatchResponse batchResponse = firebaseMessaging.sendEach(messageList);
            List<String> messageIds = batchResponse.getResponses().stream()
                    .map(SendResponse::getMessageId)
                    .toList();

            if (!messageIds.isEmpty()) {
                log.info("Successfully sent notification: {}", messageIds.get(0));
            }

            List<Notification> notificationList = new ArrayList<>();
            messageIds.forEach(messageId -> {
                Notification notificationEntity = Notification.from(request, messageId);
                notificationList.add(notificationEntity);
            });

            notificationRepository.saveAll(notificationList);
            return SendNotificationResponse.newBuilder()
                    .addAllNotificationId(messageIds)
                    .setSuccess(true)
                    .setSentAt(TimeStampUtil.toGrpcTimestamp(LocalDateTime.now()))
                    .build();

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token", e);
            throw new GrpcException(GrpcNotificationErrorCode.FCM_SEND_FAILED);
        }
    }
}