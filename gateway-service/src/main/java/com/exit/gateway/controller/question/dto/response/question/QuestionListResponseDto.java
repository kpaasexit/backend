package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.QuestionListResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record QuestionListResponseDto(
        List<QuestionListQueryResponseDto> questionListItems,
        boolean hasNext,
        Integer currentPage,
        Integer totalPageNum
) {
    public static QuestionListResponseDto from(QuestionListResponse questionListResponse) {
        return QuestionListResponseDto.builder()
                .questionListItems(questionListResponse.getQuestionsList().stream()
                        .map(QuestionListQueryResponseDto::from)
                        .toList())
                .hasNext(questionListResponse.getHasNext())
                .currentPage(questionListResponse.getCurrentPage())
                .totalPageNum(questionListResponse.getCurrentPage())
                .build();
    }
}