package com.exit.question.controller.dto.request;

public record AnswerReportRequest(
        Long responseId,
        String responseReportTitle,
        String responseReportContent
) {
}
