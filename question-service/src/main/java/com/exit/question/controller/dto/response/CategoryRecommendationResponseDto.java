package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.QuestionCategory;
import lombok.Builder;

@Builder
public record CategoryRecommendationResponse(
        Long questionCategoryId,
        String questionCategoryName
) {
    public static CategoryRecommendationResponse from(QuestionCategory questionCategory) {
        return CategoryRecommendationResponse.builder()
                .questionCategoryId(questionCategory.getQuestionCategoryId())
                .questionCategoryName(questionCategory.getQuestionCategoryName())
                .build();
    }
}
