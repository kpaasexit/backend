package com.exit.question.controller.dto.request;

public record QuestionReportRequest(
        Long questionId,
        String questionReportTitle,
        String questionReportContent
) {
}
