package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.QuestionCategory;
import lombok.Builder;

@Builder
public record CategoryRecommendationResponseDto(
        Long questionCategoryId,
        String questionCategoryName
) {
    public static CategoryRecommendationResponseDto from(QuestionCategory questionCategory) {
        return CategoryRecommendationResponseDto.builder()
                .questionCategoryId(questionCategory.getQuestionCategoryId())
                .questionCategoryName(questionCategory.getQuestionCategoryName())
                .build();
    }
}
