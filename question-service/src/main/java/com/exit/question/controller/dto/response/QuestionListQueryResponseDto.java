package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.QuestionAnswerType;

import java.time.LocalDateTime;

public record QuestionListQueryResponseDto(
        Long questionId,
        Long questionCategoryId,
        Long questionWriterId,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        Boolean questionAnswerAdopt,
        Integer answerCount,
        LocalDateTime createdAt
) {
}