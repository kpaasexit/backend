package com.exit.gateway.controller.search.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record QuestionSearchItemDto(
        Long questionId,
        Long questionCategory,
        Long questionWriterId,
        String questionWriterName,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        String questionAnswerType,
        Boolean questionAnswerAdopt,
        Integer answerCount,
        LocalDateTime createdAt
) {
}
