package com.exit.notification.domain;

import com.exit.common.domain.BaseEntity;
import com.exit.common.grpc.SendNotificationRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "createdAt", column = @Column(name = "notification_created_at"))
@AttributeOverride(name = "updatedAt", column = @Column(name = "notification_updated_at"))
public class Notifications extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50, nullable = false)
    private NotificationType notificationType;

    @Column(name = "notification_title", length = 255, nullable = false)
    private String notificationTitle;

    @Column(name = "notification_content", columnDefinition = "TEXT", nullable = false)
    private String notificationContent;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(name = "notification_is_read", nullable = false)
    private Boolean notificationIsRead = false;

    @Column(name = "notification_read_at")
    private LocalDateTime notificationReadAt;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "notification_fcm_message_id", length = 255)
    private String notificationFcmMessageId;

    @Builder
    public Notifications(NotificationType notificationType, String notificationTitle, String notificationContent, Long targetId, Long receiverId, Boolean notificationIsRead, LocalDateTime notificationReadAt, LocalDateTime notificationSentAt, String notificationFcmMessageId) {
        this.notificationType = notificationType;
        this.notificationTitle = notificationTitle;
        this.notificationContent = notificationContent;
        this.targetId = targetId;
        this.receiverId = receiverId;
        this.notificationIsRead = notificationIsRead;
        this.notificationReadAt = notificationReadAt;
        this.notificationSentAt = notificationSentAt;
        this.notificationFcmMessageId = notificationFcmMessageId;
    }

    public static Notifications from(SendNotificationRequest request) {
        return Notifications.builder()
                .notificationType(NotificationType.valueOf(request.getType()))
                .notificationTitle()
                .notificationContent()
                .targetId()
                .receiverId()
                .notificationIsRead()
                .notificationReadAt()
                .notificationSentAt()
                .notificationFcmMessageId()


    }
}
