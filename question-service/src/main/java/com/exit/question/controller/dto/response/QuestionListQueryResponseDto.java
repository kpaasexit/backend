package com.exit.question.controller.dto.response;

import com.exit.common.grpc.QuestionListItem;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.domain.question.QuestionAnswerType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

public record QuestionListQueryResponseDto(
        Long questionId,
        Long questionCategoryId,
        Long questionWriterId,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        Boolean isAnswered,
        Long answerCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static QuestionListItem toQuestionListItem(QuestionListQueryResponseDto dto, Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap) {
        QuestionListItem.Builder builder = QuestionListItem.newBuilder();

        String userProfile = userInfoMap.get(dto.questionWriterId).getUserProfile();
        if(!userProfile.isEmpty()) {
            builder.setQuestionWriterProfile(userProfile);
        }
        return builder
                .setQuestionId(dto.questionId)
                .setQuestionCategory(dto.questionCategoryId)
                .setQuestionWriterName(userInfoMap.get(dto.questionWriterId).getUserName())
                .setQuestionTitle(dto.questionTitle)
                .setQuestionContent(dto.questionContent)
                .setQuestionUrgency(dto.questionUrgency)
                .setQuestionAnswerType(dto.questionAnswerType.name())
                .setIsAnswered(dto.isAnswered)
                .setAnswerCount(dto.answerCount.intValue())
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(dto.createdAt))
                .build();
    }
}