package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionListResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record QuestionListResponseDto(
        List<QuestionListQueryResponseDto> questionList,
        boolean hasNext
) {
    public static QuestionListResponseDto from(QuestionListResponse questionListResponse) {
        return QuestionListResponseDto.builder()
                .questionList(questionListResponse.getQuestionsList().stream()
                        .map(QuestionListQueryResponseDto::from)
                        .toList())
                .hasNext(questionListResponse.getHasNext())
                .build();
    }
}