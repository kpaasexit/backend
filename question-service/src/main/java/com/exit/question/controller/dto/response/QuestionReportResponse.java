package com.exit.question.controller.dto.response;

import java.time.LocalDateTime;

public record QuestionReportResponse(
        Long questionReportId,
        Long questionId,
        String questionReportTitle,
        String questionReportContent,
        Long questionReportWriterId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
