package com.exit.gateway.controller.dto.response.quiz;

import com.exit.common.grpc.ReportQuizResponse;
import lombok.Builder;

@Builder
public record ReportQuizResponseDto(
        Long quizReportId
) {
    public static ReportQuizResponseDto from(ReportQuizResponse response) {
        return ReportQuizResponseDto.builder()
                .quizReportId(response.getQuizReportId())
                .build();
    }
}
