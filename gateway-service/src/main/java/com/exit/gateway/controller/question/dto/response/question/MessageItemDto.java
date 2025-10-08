package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.MessageItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record MessageItemDto(
        Boolean isQuestioner,
        Long messageId,
        String content,
        List<String> images,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static MessageItemDto from(MessageItem messageItem) {
        return MessageItemDto.builder()
                .isQuestioner(messageItem.getIsQuestioner())
                .messageId(messageItem.getMessageId())
                .content(messageItem.getContent())
                .images(messageItem.getImagesList())
                .createdAt(timestampToLocalDateTime(messageItem.getCreatedAt()))
                .build();
    }
}
