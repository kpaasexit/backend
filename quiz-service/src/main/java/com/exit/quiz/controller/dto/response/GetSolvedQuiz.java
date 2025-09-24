package com.exit.quiz.controller.dto.response;

public record GetSolvedQuiz(
        Long quizId,
        String quizTitle
) {
}
