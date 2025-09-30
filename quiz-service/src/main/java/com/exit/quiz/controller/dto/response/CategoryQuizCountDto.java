package com.exit.quiz.controller.dto.response;

public record CategoryQuizCountDto(
        Long categoryId,
        Integer quizTotalCount,
        Integer quizSolvedCount
) {
}
