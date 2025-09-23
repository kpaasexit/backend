package com.exit.question.controller.dto.request;

import com.exit.common.grpc.AnswerReportRequest;

public record AnswerReportRequestDto(
        Long responseId,
        Long responseReportWriterId,
        String responseReportTitle,
        String responseReportContent
) {
    public static AnswerReportRequestDto from(AnswerReportRequest request) {
        return new AnswerReportRequestDto(
                request.getResponseId(),
                request.getResponseReportWriterId(),
                request.getResponseReportTitle(),
                request.getResponseReportContent()
        );
    }
}
