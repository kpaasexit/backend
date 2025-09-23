package com.exit.question.controller.dto.response;

public record AnswerRecommendResponseDto(
        int answerLikeNum,
        boolean isLiked
) {
}