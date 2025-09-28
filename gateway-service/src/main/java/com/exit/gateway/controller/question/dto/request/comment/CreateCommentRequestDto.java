package com.exit.gateway.controller.question.dto.request.comment;

public record CreateCommentRequestDto(
        Long targetId,
        String commentType,
        String content
) {
}
