package com.exit.question.controller.dto.request;

import com.exit.common.grpc.AnswerRecommendRequest;

public record AnswerRecommendRequestDto(
        Long responseId,
        Long userId
) {
    public static AnswerRecommendRequestDto from(AnswerRecommendRequest request) {
        return new AnswerRecommendRequestDto(
                request.getResponseId(),
                request.getUserId()
        );
    }
}
