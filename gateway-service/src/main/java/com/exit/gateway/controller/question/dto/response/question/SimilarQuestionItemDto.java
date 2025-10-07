package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.SimilarQuestionItem;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record SimilarQuestionItemDto(
        Long questionId,
        String questionTitle,
        String questionContent,
        Long questionCategory,
        Boolean questionUrgency,
        String questionAnswerType,
        Boolean questionAnswerAdopt,
        LocalDateTime createdAt
) {
    public static SimilarQuestionItemDto from(SimilarQuestionItem SimilarQuestionItem) {
        return SimilarQuestionItemDto.builder()
                .questionId(SimilarQuestionItem.getQuestionId())
                .questionTitle(SimilarQuestionItem.getQuestionTitle())
                .questionContent(SimilarQuestionItem.getQuestionContent())
                .questionCategory(SimilarQuestionItem.getQuestionCategory())
                .questionUrgency(SimilarQuestionItem.getQuestionUrgency())
                .questionAnswerType(SimilarQuestionItem.getQuestionAnswerType())
                .questionAnswerAdopt(SimilarQuestionItem.getQuestionAnswerAdopt())
                .createdAt(timestampToLocalDateTime(SimilarQuestionItem.getCreatedAt()))
                .build();
    }
}
