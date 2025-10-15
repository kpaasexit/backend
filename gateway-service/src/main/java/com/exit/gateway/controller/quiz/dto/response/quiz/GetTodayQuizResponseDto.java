package com.exit.gateway.controller.quiz.dto.response.quiz;

import com.exit.common.grpc.GetTodayQuizResponse;
import lombok.Builder;

@Builder
public record GetTodayQuizResponseDto(
        Long quizId
) {
    public static GetTodayQuizResponseDto from(GetTodayQuizResponse response) {
        return GetTodayQuizResponseDto.builder()
                .quizId(response.getQuizId())
                .build();
    }
}
