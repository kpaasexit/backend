package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.SimilarQuestionResponse;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record SimilarQuestionResponseDto(
        Long questionId,
        String questionTitle,
        String questionContent,
        Long questionCategory,
        Boolean questionUrgency,
        String questionAnswerType,
        Boolean questionAnswerAdopt,
        LocalDateTime createdAt
) {
    public static SimilarQuestionResponseDto from(SimilarQuestionResponse similarQuestionResponse) {
        return SimilarQuestionResponseDto.builder()
                .questionId(similarQuestionResponse.getQuestionId())
                .questionTitle(similarQuestionResponse.getQuestionTitle())
                .questionContent(similarQuestionResponse.getQuestionContent())
                .questionCategory(similarQuestionResponse.getQuestionCategory())
                .questionUrgency(similarQuestionResponse.getQuestionUrgency())
                .questionAnswerType(similarQuestionResponse.getQuestionAnswerType())
                .questionAnswerAdopt(similarQuestionResponse.getQuestionAnswerAdopt())
                .createdAt(timestampToLocalDateTime(similarQuestionResponse.getCreatedAt()))
                .build();
    }
}