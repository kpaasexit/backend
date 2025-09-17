package com.exit.question.controller.dto.response;

public record AnswerRecommendResponse(
        int answerLikeNum,
        boolean isLiked
) {
}