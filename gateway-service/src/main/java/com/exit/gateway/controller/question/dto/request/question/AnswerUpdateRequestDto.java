package com.exit.gateway.controller.question.dto.request.question;

import jakarta.validation.constraints.NotBlank;

public record AnswerUpdateRequestDto(
        @NotBlank(message = "답변 내용은 필수입니다.")
        String content
) {
}