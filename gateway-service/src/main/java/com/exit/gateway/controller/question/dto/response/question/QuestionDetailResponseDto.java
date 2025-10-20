package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionDetailResponse;
import com.exit.gateway.controller.question.dto.response.authority.QuestionAuthority;

public record QuestionDetailResponseDto(
        QuestionCreateResponseDto question,
        QuestionAuthority authority
) {
    public static QuestionDetailResponseDto from(QuestionDetailResponse grpcResponse) {
        QuestionCreateResponseDto questionDto = QuestionCreateResponseDto.from(grpcResponse.getQuestion());

        return new QuestionDetailResponseDto(
                questionDto,
                QuestionAuthority.from(grpcResponse.getAuthority())
        );
    }
}