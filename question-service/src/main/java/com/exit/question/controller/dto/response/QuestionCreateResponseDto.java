package com.exit.question.controller.dto.response;

import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionAnswerType;
import com.exit.question.domain.question.QuestionDisclosureType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record QuestionCreateResponse(
        Long questionId,
        Long questionWriterId,
        String questionTitle,
        String questionContent,
        Long questionCategory,
        String questionUrgency,
        String questionAnswerType,
        String questionDisclosureType,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static QuestionCreateResponse from(Question savedQuestion, List<String> urls) {
        QuestionCreateResponseBuilder builder = QuestionCreateResponse.builder();

        if(urls != null && !urls.isEmpty()){
            builder.imageUrls(urls);
        }

        return builder
                .questionId(savedQuestion.getQuestionId())
                .questionWriterId(savedQuestion.getQuestionWriterId())
                .questionTitle(savedQuestion.getQuestionTitle())
                .questionContent(savedQuestion.getQuestionContent())
                .questionCategory(savedQuestion.getQuestionCategory().getQuestionCategoryId())
                .questionUrgency(savedQuestion.getQuestionUrgency() ? "HIGH" : "LOW")
                .questionAnswerType(savedQuestion.getQuestionAnswerType().name())
                .questionDisclosureType(savedQuestion.getQuestionDisclosure().name())
                .createdAt(savedQuestion.getCreatedAt())
                .build();
    }
}
