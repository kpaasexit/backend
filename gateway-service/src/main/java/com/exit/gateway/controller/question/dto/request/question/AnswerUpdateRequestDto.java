package com.exit.gateway.controller.question.dto.request.question;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AnswerUpdateRequestDto(
        @NotBlank(message = "답변 내용은 필수입니다.")
        String content,
        List<Long> deleteIds
) {
}