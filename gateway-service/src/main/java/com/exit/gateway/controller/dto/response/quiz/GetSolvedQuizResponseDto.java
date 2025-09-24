package com.exit.gateway.controller.dto.response.quiz;

import com.exit.common.grpc.AttemptQuiz;
import com.exit.common.grpc.GetSolvedQuizResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record GetSolvedQuizResponseDto(
        List<AttemptQuizDto> attemptQuiz,
        boolean hasNext
) {
    public static GetSolvedQuizResponseDto from(GetSolvedQuizResponse response) {
        List<AttemptQuizDto> attemptQuizList = response.getAttemptQuizList().stream()
                .map(AttemptQuizDto::from)
                .toList();

        return GetSolvedQuizResponseDto.builder()
                .attemptQuiz(attemptQuizList)
                .hasNext(response.getHasNext())
                .build();
    }

    @Builder
    public record AttemptQuizDto(
            Long quizId,
            String quizTitle
    ) {
        public static AttemptQuizDto from(AttemptQuiz attemptQuiz) {
            return AttemptQuizDto.builder()
                    .quizId(attemptQuiz.getQuizId())
                    .quizTitle(attemptQuiz.getQuizTitle())
                    .build();
        }
    }
}