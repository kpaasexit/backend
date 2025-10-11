package com.exit.question.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record PopularPostDto(
        Long questionId,
        Long questionCategoryId,
        Long questionWriterId,
        String questionTitle,
        String questionContent,
        Boolean questionAnswerAdopt,
        Integer answerCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
}
