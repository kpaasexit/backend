package com.exit.gateway.controller.dto.response.question;

import com.exit.common.grpc.QuestionDetailResponse;

import java.util.List;

public record QuestionDetailResponseDto(
        QuestionCreateResponseDto question,
        List<ResponseDetailDto> responses,
        Boolean hasNext
) {
    public static QuestionDetailResponseDto from(QuestionDetailResponse grpcResponse) {
        QuestionCreateResponseDto questionDto = QuestionCreateResponseDto.from(grpcResponse.getQuestion());

        List<ResponseDetailDto> responseDtos = grpcResponse.getResponsesList().stream()
                .map(ResponseDetailDto::from)
                .toList();

        return new QuestionDetailResponseDto(
                questionDto,
                responseDtos,
                grpcResponse.getHasNext()
        );
    }
}