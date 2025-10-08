package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionReportResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
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
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
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