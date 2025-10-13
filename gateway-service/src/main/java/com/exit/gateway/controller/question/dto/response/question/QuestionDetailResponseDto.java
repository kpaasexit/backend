package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionDetailResponse;

public record QuestionDetailResponseDto(
        QuestionCreateResponseDto question
) {
    public static QuestionDetailResponseDto from(QuestionDetailResponse grpcResponse) {
        QuestionCreateResponseDto questionDto = QuestionCreateResponseDto.from(grpcResponse.getQuestion());

        return new QuestionDetailResponseDto(
                questionDto
        );
    }
}