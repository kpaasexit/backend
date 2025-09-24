package com.exit.gateway.controller.quiz.dto.response.quiz;

import com.exit.common.grpc.GetQuizResponse;
import lombok.Builder;

@Builder
public record GetQuizResponseDto(
        Long quizId,
        Long quizCategoryId,
        String quizTitle,
        String quizContent,
        String quizType,
        String quizCorrectAnswer,
        String quizAdditionalInformation
) {
    public static GetQuizResponseDto from(GetQuizResponse response) {
        return GetQuizResponseDto.builder()
                .quizId(response.getQuizId())
                .quizCategoryId(response.getQuizCategoryId())
                .quizTitle(response.getQuizTitle())
                .quizContent(response.getQuizContent())
                .quizType(response.getQuizType())
                .quizCorrectAnswer(response.getQuizCorrectAnswer())
                .quizAdditionalInformation(response.getQuizAdditionalInformation())
                .build();
    }
}
