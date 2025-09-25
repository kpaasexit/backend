package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.CategoryRecommendationResponse;
import lombok.Builder;

@Builder
public record CategoryRecommendationResponseDto(
        Long questionCategoryId,
        String questionCategoryName
) {
    public static CategoryRecommendationResponseDto from(CategoryRecommendationResponse categoryRecommendationResponse) {
        return CategoryRecommendationResponseDto.builder()
                .questionCategoryId(categoryRecommendationResponse.getCategoryId())
                .questionCategoryName(categoryRecommendationResponse.getCategoryName())
                .build();
    }
}