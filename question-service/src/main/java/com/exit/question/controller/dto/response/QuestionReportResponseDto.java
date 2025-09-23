package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.QuestionReport;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record QuestionReportResponseDto(
        Long questionReportId,
        Long questionId,
        String questionReportTitle,
        String questionReportContent,
        Long questionReportWriterId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuestionReportResponseDto from(QuestionReport questionReport) {
        return QuestionReportResponseDto.builder()
                .questionReportId(questionReport.getQuestionReportId())
                .questionId(questionReport.getQuestionId())
                .questionReportTitle(questionReport.getQuestionReportTitle())
                .questionReportContent(questionReport.getQuestionReportContent())
                .questionReportWriterId(questionReport.getQuestionReportWriterId())
                .createdAt(questionReport.getCreatedAt())
                .updatedAt(questionReport.getUpdatedAt())
                .build();
    }
}