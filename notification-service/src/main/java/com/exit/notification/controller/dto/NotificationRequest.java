package com.exit.notification.controller.dto;

import com.exit.notification.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String token;

    @NotBlank(message = "알림 제목은 필수입니다")
    private String title;

    @NotBlank(message = "알림 내용은 필수입니다")
    private String body;

    @NotNull(message = "알림 타입은 필수입니다")
    private NotificationType type;

    private Long targetId;

    private Long receiverId;
}