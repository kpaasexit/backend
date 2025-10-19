package com.exit.gateway.controller.quiz.dto.response.quiz;

import com.exit.common.grpc.AttemptQuiz;
import com.exit.common.grpc.GetSolvedQuizResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record GetSolvedQuizResponseDto(
        List<AttemptQuizDto> attemptQuiz,
        boolean hasNext,
        Integer currentPage,
        Integer totalPageNum
) {
    public static GetSolvedQuizResponseDto from(GetSolvedQuizResponse response) {
        List<AttemptQuizDto> attemptQuizList = response.getAttemptQuizList().stream()
                .map(AttemptQuizDto::from)
                .toList();

        return GetSolvedQuizResponseDto.builder()
                .attemptQuiz(attemptQuizList)
                .hasNext(response.getHasNext())
                .currentPage(response.getCurrentPage())
                .totalPageNum(response.getTotalPageNum())
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