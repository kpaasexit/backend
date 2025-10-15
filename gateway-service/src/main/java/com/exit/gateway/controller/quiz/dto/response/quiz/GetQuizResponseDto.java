package com.exit.gateway.controller.quiz.dto.response.quiz;

import com.exit.common.grpc.GetQuizResponse;
import lombok.Builder;

@Builder
public record GetQuizResponseDto(
        Long id,
        Long categoryId,
        String question,
        String type,
        String[] options,
        String correctAnswer,
        String explanation
) {
    public static GetQuizResponseDto from(GetQuizResponse response) {
        GetQuizResponseDtoBuilder builder = GetQuizResponseDto.builder();

        if(!response.getQuizContentList().isEmpty()){
            builder.options(response.getQuizContentList().toArray(String[]::new));
        }

        return builder
                .id(response.getQuizId())
                .categoryId(response.getQuizCategoryId())
                .question(response.getQuizTitle())
                .type(response.getQuizType())
                .correctAnswer(response.getQuizCorrectAnswer())
                .explanation(response.getQuizAdditionalInformation())
                .build();
    }
}
