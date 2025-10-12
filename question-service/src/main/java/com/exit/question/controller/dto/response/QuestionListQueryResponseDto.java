package com.exit.question.controller.dto.response;

import com.exit.common.grpc.QuestionListItem;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.domain.question.QuestionAnswerType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record QuestionListQueryResponseDto(
        Long questionId,
        Long questionCategoryId,
        Long questionWriterId,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        Boolean questionAnswerAdopt,
        Long answerCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static QuestionListItem toQuestionListItem(QuestionListQueryResponseDto dto) {
        return QuestionListItem.newBuilder()
                .setQuestionId(dto.questionId)
                .setQuestionCategory(dto.questionCategoryId)
                .setQuestionTitle(dto.questionTitle)
                .setQuestionWriterId(dto.questionWriterId)
                .setQuestionContent(dto.questionContent)
                .setQuestionUrgency(dto.questionUrgency)
                .setQuestionAnswerType(dto.questionAnswerType.name())
                .setQuestionAnswerAdopt(dto.questionAnswerAdopt)
                .setAnswerCount(dto.answerCount.intValue())
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(dto.createdAt))
                .build();
    }
}