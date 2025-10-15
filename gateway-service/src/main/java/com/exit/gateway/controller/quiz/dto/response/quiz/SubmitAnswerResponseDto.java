package com.exit.gateway.controller.quiz.dto.response.quiz;

import com.exit.common.grpc.SubmitAnswerResponse;
import lombok.Builder;

@Builder
public record SubmitAnswerResponseDto(
        Boolean hasNext
) {
    public static SubmitAnswerResponseDto from(SubmitAnswerResponse response) {
        return SubmitAnswerResponseDto.builder()
                .hasNext(response.getHasNext())
                .build();
    }
}
