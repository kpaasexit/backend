package com.exit.question.controller.dto.response;

import com.exit.common.grpc.AiBestResponse;

public record AiBestResponseDto(
        String title,
        String content,
        Long questionId,
        Long responseId
) {
    public AiBestResponse toGrpc(){
        return AiBestResponse.newBuilder()
                .setTitle(title)
                .setContent(content)
                .setQuestionId(questionId)
                .setResponseId(responseId)
                .build();
    }
}
