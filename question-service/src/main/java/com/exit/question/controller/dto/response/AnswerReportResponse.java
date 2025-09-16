package com.exit.question.controller.dto.response;

import java.time.LocalDateTime;

public record AnswerReportResponse(
        Long responseReportId,
        Long responseId,
        String responseReportTitle,
        String responseReportContent,
        Long responseReportWriterId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
