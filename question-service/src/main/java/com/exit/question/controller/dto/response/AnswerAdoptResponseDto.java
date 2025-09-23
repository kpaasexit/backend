package com.exit.question.controller.dto.response;

import com.exit.question.domain.response.Response;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AnswerAdoptResponseDto(
        Long responseId,
        Boolean responseAdopt,
        LocalDateTime updatedAt
) {
    public static AnswerAdoptResponseDto from(Response response) {
        return AnswerAdoptResponseDto.builder()
                .responseId(response.getResponseId())
                .responseAdopt(response.getResponseAdopt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }

}
