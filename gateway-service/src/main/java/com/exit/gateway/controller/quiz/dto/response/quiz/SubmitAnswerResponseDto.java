package com.exit.gateway.controller.quiz.dto.response.quiz;

import com.exit.common.grpc.SubmitAnswerResponse;
import lombok.Builder;

@Builder
public record SubmitAnswerResponseDto(
        Long quizId,
        Integer quizTotalAttemptNum,
        Integer quizCorrectNum,
        Double quizCorrectPercent
) {
    public static SubmitAnswerResponseDto from(SubmitAnswerResponse response) {
        return SubmitAnswerResponseDto.builder()
                .quizId(response.getQuizId())
                .quizTotalAttemptNum(response.getQuizTotalAttemptNum())
                .quizCorrectNum(response.getQuizCorrectNum())
                .quizCorrectPercent(response.getQuizCorrectPercent())
                .build();
    }
}
