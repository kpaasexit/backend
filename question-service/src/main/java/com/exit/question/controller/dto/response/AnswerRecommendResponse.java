package com.exit.question.controller.dto.response;

import java.time.LocalDateTime;

public record AnswerRecommendResponse(
        Long responseLikeId,
        Long responseId,
        Long userId,
        LocalDateTime createdAt
) {
}
