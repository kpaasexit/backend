package com.exit.question.controller.dto.request;

public record NotificationContentDto(
        String content,
        Long receiverId
) {
}
