package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionListItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record QuestionListQueryResponseDto(
        Long questionId,
        Long questionCategoryId,
        String questionWriterName,
        String questionWriterProfile,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        String questionAnswerType,
        Boolean isAnswered,
        Integer answerCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static QuestionListQueryResponseDto from(QuestionListItem questionListItem) {
        QuestionListQueryResponseDtoBuilder builder = QuestionListQueryResponseDto.builder();
        if(!questionListItem.getQuestionWriterProfile().isEmpty()) {
            builder.questionWriterProfile(questionListItem.getQuestionWriterProfile());
        }

        return builder
                .questionId(questionListItem.getQuestionId())
                .questionCategoryId(questionListItem.getQuestionCategory())
                .questionWriterName(questionListItem.getQuestionWriterName())
                .questionTitle(questionListItem.getQuestionTitle())
                .questionContent(questionListItem.getQuestionContent())
                .questionUrgency(questionListItem.getQuestionUrgency())
                .questionAnswerType(questionListItem.getQuestionAnswerType())
                .isAnswered(questionListItem.getIsAnswered())
                .answerCount(questionListItem.getAnswerCount())
                .createdAt(timestampToLocalDateTime(questionListItem.getCreatedAt()))
                .build();
    }
}
