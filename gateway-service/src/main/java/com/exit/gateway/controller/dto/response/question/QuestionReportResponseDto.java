package com.exit.gateway.controller.dto.response.question;

import com.exit.common.grpc.QuestionReportResponse;
import com.exit.common.util.time.TimeStampUtil;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record QuestionReportResponseDto(
        Long questionId,
        Long questionReportId,
        String questionReportTitle,
        String questionReportContent,
        Long questionReportWriterId,
        LocalDateTime createdAt
) {
    public static QuestionReportResponseDto from(QuestionReportResponse questionReportResponse) {
        return QuestionReportResponseDto.builder()
                .questionId(questionReportResponse.getQuestionId())
                .questionReportId(questionReportResponse.getQuestionReportId())
                .questionReportTitle(questionReportResponse.getQuestionReportTitle())
                .questionReportContent(questionReportResponse.getQuestionReportContent())
                .questionReportWriterId(questionReportResponse.getQuestionReportWriterId())
                .createdAt(timestampToLocalDateTime(questionReportResponse.getCreatedAt()))
                .build();
    }
}