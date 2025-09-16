package com.exit.question.controller.dto.response;

import java.time.LocalDateTime;

public record HashtagSuggestionResponse(
        Long hashTagId,
        Long questionId,
        String hashTagTitle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
