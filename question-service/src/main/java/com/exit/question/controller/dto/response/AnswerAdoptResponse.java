package com.exit.question.controller.dto.response;

import java.time.LocalDateTime;

public record AnswerAdoptResponse(
        Long responseId,
        Boolean responseAdopt,
        LocalDateTime updatedAt
) {
}
