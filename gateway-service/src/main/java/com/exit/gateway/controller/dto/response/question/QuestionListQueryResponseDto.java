package com.exit.gateway.controller.dto.response.question;

import com.exit.common.grpc.QuestionListItem;
import com.google.protobuf.Timestamp;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record QuestionListQueryResponseDto(
        Long questionId,
        Long questionCategoryId,
        Long questionWriterId,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        String questionAnswerType,
        Boolean questionAnswerAdopt,
        Integer answerCount,
        LocalDateTime createdAt
) {
    public static QuestionListQueryResponseDto from(QuestionListItem questionListItem) {
        return QuestionListQueryResponseDto.builder()
                .questionId(questionListItem.getQuestionId())
                .questionCategoryId(questionListItem.getQuestionCategory())
                .questionWriterId(questionListItem.getQuestionWriterId())
                .questionTitle(questionListItem.getQuestionTitle())
                .questionContent(questionListItem.getQuestionContent())
                .questionUrgency(questionListItem.getQuestionUrgency())
                .questionAnswerType(questionListItem.getQuestionAnswerType())
                .questionAnswerAdopt(questionListItem.getQuestionAnswerAdopt())
                .answerCount(questionListItem.getAnswerCount())
                .createdAt(timestampToLocalDateTime(questionListItem.getCreatedAt()))
                .build();
    }
}
