package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.Question;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record QuestionCreateResponseDto(
        Long questionId,
        Long questionWriterId,
        String questionWriterName,
        String questionTitle,
        String questionContent,
        Long questionCategory,
        Boolean questionUrgency,
        String questionAnswerType,
        String questionDisclosureType,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static QuestionCreateResponseDto from(Question savedQuestion, List<String> urls, String questionWriterName) {
        QuestionCreateResponseDtoBuilder builder = QuestionCreateResponseDto.builder();

        if(urls != null && !urls.isEmpty()){
            builder.imageUrls(urls);
        }

        return builder
                .questionId(savedQuestion.getQuestionId())
                .questionWriterId(savedQuestion.getQuestionWriterId())
                .questionWriterName(questionWriterName)
                .questionTitle(savedQuestion.getQuestionTitle())
                .questionContent(savedQuestion.getQuestionContent())
                .questionCategory(savedQuestion.getQuestionCategory().getQuestionCategoryId())
                .questionUrgency(savedQuestion.getQuestionUrgency())
                .questionAnswerType(savedQuestion.getQuestionAnswerType().name())
                .questionDisclosureType(savedQuestion.getQuestionDisclosure().name())
                .createdAt(savedQuestion.getCreatedAt())
                .build();
    }
}
