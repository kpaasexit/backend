package com.exit.gateway.controller.dto.response.question;

import com.exit.common.grpc.AnswerAdoptResponse;
import lombok.Builder;

@Builder
public record AnswerAdoptResponseDto(
        Long responseId,
        Boolean isAdopted
) {
    public static AnswerAdoptResponseDto from(AnswerAdoptResponse answerAdoptResponse) {
        return AnswerAdoptResponseDto.builder()
                .responseId(answerAdoptResponse.getResponseId())
                .isAdopted(answerAdoptResponse.getResponseAdopt())
                .build();
    }
}