package com.exit.gateway.controller.dto.response.question;

import com.exit.common.grpc.AnswerCreateResponse;
import com.google.protobuf.Timestamp;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record AnswerCreateResponseDto(
        Long responseId,
        String responseContent,
        Long questionId,
        Long responseWriterId,
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