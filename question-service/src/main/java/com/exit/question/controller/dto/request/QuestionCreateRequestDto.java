package com.exit.question.controller.dto.request;

import com.exit.common.grpc.QuestionCreateRequest;
import com.exit.common.grpc.UploadBytesRequest;
import com.exit.question.domain.question.QuestionAnswerType;
import com.exit.question.domain.question.QuestionDisclosureType;
import lombok.Builder;

import java.util.List;

@Builder
public record QuestionCreateRequestDto(
        Long questionCategoryId,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        QuestionDisclosureType questionDisclosure,
        Long questionWriterId,
        Boolean questionIsAnonymous,
        List<UploadBytesRequest> images
) {
    public static QuestionCreateRequestDto from(QuestionCreateRequest request) {
        return QuestionCreateRequestDto.builder()
                .questionCategoryId(request.getQuestionCategory())
                .questionTitle(request.getQuestionTitle())
                .questionContent(request.getQuestionContent())
                .questionUrgency(request.getQuestionUrgency())
                .questionAnswerType(QuestionAnswerType.valueOf(request.getQuestionAnswerType()))
                .questionDisclosure(QuestionDisclosureType.valueOf(request.getQuestionDisclosureType()))
                .questionWriterId(request.getQuestionWriterId())
                .questionIsAnonymous(request.getQuestionIsAnonymous())
                .images(request.getImagesList())
                .build();
    }
}