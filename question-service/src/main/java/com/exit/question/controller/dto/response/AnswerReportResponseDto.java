package com.exit.question.controller.dto.response;

import com.exit.question.domain.response.ResponseReport;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AnswerReportResponseDto(
        Long responseReportId,
        Long responseId,
        String responseReportTitle,
        String responseReportContent,
        Long responseReportWriterId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnswerReportResponseDto from(ResponseReport request) {
        return AnswerReportResponseDto.builder()
                .responseReportId(request.getResponseReportId())
                .responseId(request.getResponseId())
                .responseReportTitle(request.getResponseReportTitle())
                .responseReportContent(request.getResponseReportContent())
                .responseReportWriterId(request.getResponseReportWriterId())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
