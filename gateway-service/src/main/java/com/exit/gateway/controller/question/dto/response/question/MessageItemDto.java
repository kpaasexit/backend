package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.MessageItem;
import com.exit.gateway.controller.question.dto.ImageObjectDto;
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
        List<ImageObjectDto> images,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static MessageItemDto from(MessageItem messageItem) {
        MessageItemDtoBuilder builder = MessageItemDto.builder();

        if (!messageItem.getImagesList().isEmpty()) {
            List<ImageObjectDto> images = messageItem.getImagesList().stream().map(
                    imageObject -> {
                        return ImageObjectDto.builder()
                                .imageId(imageObject.getImageId())
                                .imageUrl(imageObject.getImageUrl())
                                .build();
                    }
            ).toList();
            builder.images(images);
        }

        return builder
                .isQuestioner(messageItem.getIsQuestioner())
                .messageId(messageItem.getMessageId())
                .content(messageItem.getContent())
                .createdAt(timestampToLocalDateTime(messageItem.getCreatedAt()))
                .build();
    }
}
