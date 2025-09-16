package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.QuestionAnswerType;
import com.exit.question.domain.question.QuestionCategoryType;
import java.time.LocalDateTime;
import java.util.List;

public record QuestionListResponse(
        Long questionId,
        Long questionCategoryId,
        Long questionWriterId,
        String questionTitle,
        String questionContent,
        QuestionCategoryType questionCategory,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        Boolean questionAnswerAdopt,
        List<String> hashTags,
        List<String> imageUrls,
        Integer answerCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
