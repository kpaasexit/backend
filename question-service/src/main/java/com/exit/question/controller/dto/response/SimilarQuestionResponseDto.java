package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.QuestionAnswerType;
import com.exit.question.domain.question.QuestionCategory;

import java.time.LocalDateTime;
import java.util.List;

public record SimilarQuestionResponse(
        Long questionId,
        String questionTitle,
        String questionContent,
        QuestionCategory questionCategory,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        Boolean questionAnswerAdopt,
        LocalDateTime createdAt
) {
}
