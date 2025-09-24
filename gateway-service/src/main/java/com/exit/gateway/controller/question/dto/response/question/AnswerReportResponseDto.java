package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.AnswerReportResponse;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record AnswerReportResponseDto(
        Long responseReportId,
        Long responseId,
        String responseReportTitle,
        String responseReportContent,
        Long responseReportWriterId,
        LocalDateTime createdAt
) {
    public static AnswerReportResponseDto from(AnswerReportResponse answerReportResponse) {
        return AnswerReportResponseDto.builder()
                .responseId(answerReportResponse.getResponseId())
                .responseReportId(answerReportResponse.getResponseReportId())
                .responseReportTitle(answerReportResponse.getResponseReportTitle())
                .responseReportContent(answerReportResponse.getResponseReportContent())
                .responseReportWriterId(answerReportResponse.getResponseReportWriterId())
                .createdAt(timestampToLocalDateTime(answerReportResponse.getCreatedAt()))
                .build();
    }
}