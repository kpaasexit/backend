package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.AnswerCreateResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record AnswerCreateResponseDto(
        Long responseId,
        String responseContent,
        Long questionId,
        Long responseWriterId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static AnswerCreateResponseDto from(AnswerCreateResponse answerCreateResponse) {
        return AnswerCreateResponseDto.builder()
                .responseId(answerCreateResponse.getResponseId())
                .responseContent(answerCreateResponse.getResponseContent())
                .questionId(answerCreateResponse.getQuestionId())
                .responseWriterId(answerCreateResponse.getResponseWriterId())
                .createdAt(timestampToLocalDateTime(answerCreateResponse.getCreatedAt()))
                .build();
    }
}