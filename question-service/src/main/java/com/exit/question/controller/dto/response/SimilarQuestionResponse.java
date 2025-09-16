package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.QuestionAnswerType;
import com.exit.question.domain.question.QuestionCategoryType;
import java.time.LocalDateTime;
import java.util.List;

public record SimilarQuestionResponse(
        Long questionId,
        String questionTitle,
        String questionContent,
        QuestionCategoryType questionCategory,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        Boolean questionAnswerAdopt,
        List<String> hashTags,
        Double similarityScore,
        LocalDateTime createdAt
) {
}
