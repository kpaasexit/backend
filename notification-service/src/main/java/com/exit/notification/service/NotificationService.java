package com.exit.notification.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.common.grpc.SendNotificationResponse;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.notification.domain.Notification;
import com.exit.notification.domain.NotificationType;
import com.exit.notification.domain.repository.NotificationRepository;
import com.exit.notification.exception.GrpcNotificationErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

            String fcmToken = userGrpcClient.getFcmToken(request.getReceiverId(), request.getDeviceId());
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)
                    .build();

            String fcmMessageId = firebaseMessaging.send(message);
            log.info("Successfully sent notification: {}", fcmMessageId);

            Notification notificationEntity = Notification.from(request, fcmMessageId);
            notificationRepository.save(notificationEntity);

            return SendNotificationResponse.newBuilder()
                    .setNotificationId(fcmMessageId)
                    .setSuccess(true)
                    .setSentAt(TimeStampUtil.toGrpcTimestamp(notificationEntity.getNotificationSentAt()))
                    .build();

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send notification to token", e);
            throw new GrpcException(GrpcNotificationErrorCode.FCM_SEND_FAILED);
        }
    }
}