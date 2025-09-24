package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionCreateResponse;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record QuestionCreateResponseDto(
        Long questionId,
        String questionTitle,
        String questionContent,
        Long questionCategory,
        Boolean questionUrgency,
        String questionAnswerType,
        String questionDisclosureType,
        Long questionWriterId,
        String questionWriterName,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static QuestionCreateResponseDto from(QuestionCreateResponse questionCreateResponse) {
        QuestionCreateResponseDtoBuilder builder = QuestionCreateResponseDto.builder();
        if(!questionCreateResponse.getImageUrlsList().isEmpty()) {
            builder.imageUrls(questionCreateResponse.getImageUrlsList());
        }

        return builder
                .questionId(questionCreateResponse.getQuestionId())
                .questionTitle(questionCreateResponse.getQuestionTitle())
                .questionContent(questionCreateResponse.getQuestionContent())
                .questionCategory(questionCreateResponse.getQuestionCategory())
                .questionUrgency(questionCreateResponse.getQuestionUrgency())
                .questionAnswerType(questionCreateResponse.getQuestionAnswerType())
                .questionDisclosureType(questionCreateResponse.getQuestionDisclosureType())
                .questionWriterId(questionCreateResponse.getQuestionWriterId())
                .questionWriterName(questionCreateResponse.getQuestionWriterName())
                .createdAt(timestampToLocalDateTime(questionCreateResponse.getCreatedAt()))
                .build();
    }
}
