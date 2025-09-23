package com.exit.question.controller.dto.response;

import java.time.LocalDateTime;

public record HashtagSuggestionResponseDto(
        Long hashTagId,
        Long questionId,
        String hashTagTitle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
