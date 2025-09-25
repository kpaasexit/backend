package com.exit.gateway.controller.quiz.dto.response.quiz;

import com.exit.common.grpc.GetCategoryStatisticsResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record GetCategoryStatisticsResponseDto(
        Long categoryId,
        Integer quizTotalNum,
        Integer quizSolvedNum
) {
    public static List<GetCategoryStatisticsResponseDto> from(GetCategoryStatisticsResponse response) {
        return response.getCategoryStatList().stream()
                .map(stat ->
                        GetCategoryStatisticsResponseDto.builder()
                                .categoryId(stat.getCategoryId())
                                .quizTotalNum(stat.getCategoryQuizNum())
                                .quizSolvedNum(stat.getCategorySolvedNum())
                                .build())
                .toList();
    }
}
