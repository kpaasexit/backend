package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.CreateCommentResponse;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record CreateCommentResponseDto(
        Long commentId,
        String content,
        String userName,
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
