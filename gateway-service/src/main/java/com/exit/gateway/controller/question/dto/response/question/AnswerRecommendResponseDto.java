package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.AnswerRecommendResponse;
import lombok.Builder;

@Builder
public record AnswerRecommendResponseDto(
        Long responseId,
        Boolean isRecommended,
        Integer recommendCount
) {
    public static AnswerRecommendResponseDto from(AnswerRecommendResponse answerRecommendResponse) {
        return AnswerRecommendResponseDto.builder()
                .responseId(answerRecommendResponse.getResponseId())
                .recommendCount(answerRecommendResponse.getCount())
                .isRecommended(answerRecommendResponse.getIsRecommended())
                .build();
    }
}