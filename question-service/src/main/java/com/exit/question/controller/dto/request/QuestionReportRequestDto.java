package com.exit.question.controller.dto.request;

import com.exit.common.grpc.QuestionReportRequest;

public record QuestionReportRequestDto(
        Long questionId,
        Long questionReportWriterId,
        String questionReportTitle,
        String questionReportContent
) {
    public static QuestionReportRequestDto from(QuestionReportRequest request) {
        return new QuestionReportRequestDto(
                request.getQuestionId(),
                request.getQuestionReportWriterId(),
                request.getQuestionReportTitle(),
                request.getQuestionReportContent()
        );
    }
}
