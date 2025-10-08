package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.CreateCommentResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record CreateCommentResponseDto(
        Long commentId,
        String content,
        String userName,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static CreateCommentResponseDto from(CreateCommentResponse response) {
        return CreateCommentResponseDto.builder()
                .commentId(response.getCommentId())
                .content(response.getContent())
                .userName(response.getUserName())
                .createdAt(timestampToLocalDateTime(response.getCreatedAt()))
                .build();
    }
}
