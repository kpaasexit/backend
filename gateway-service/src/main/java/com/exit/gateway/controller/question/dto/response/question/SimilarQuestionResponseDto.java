package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.SimilarQuestionResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record SimilarQuestionResponseDto(
        List<SimilarQuestionItemDto> similarQuestionResponseDto
) {
    public static SimilarQuestionResponseDto from(SimilarQuestionResponse similarQuestionResponse) {
        List<SimilarQuestionItemDto> similarQuestionItems = similarQuestionResponse.getSimilarQuestionsList().stream()
                .map(SimilarQuestionItemDto::from)
                .toList();

        return SimilarQuestionResponseDto.builder()
                .similarQuestionResponseDto(similarQuestionItems)
                .build();
    }
}