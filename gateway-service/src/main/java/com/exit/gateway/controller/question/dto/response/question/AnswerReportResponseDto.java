package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.AnswerReportResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record AnswerReportResponseDto(
        Long responseReportId,
        Long responseId,
        Integer responseReportReason,
        String responseReportContent,
        Long responseReportWriterId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static AnswerReportResponseDto from(AnswerReportResponse answerReportResponse) {
        return AnswerReportResponseDto.builder()
                .responseId(answerReportResponse.getResponseId())
                .responseReportId(answerReportResponse.getResponseReportId())
                .responseReportReason(answerReportResponse.getResponseReportReason())
                .responseReportContent(answerReportResponse.getResponseReportContent())
                .responseReportWriterId(answerReportResponse.getResponseReportWriterId())
                .createdAt(timestampToLocalDateTime(answerReportResponse.getCreatedAt()))
                .build();
    }
}