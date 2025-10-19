package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.GetDetailResponseResponse;
import lombok.Builder;

import java.util.List;

@Builder
public record GetDetailResponseResponseDto(
        List<ResponseDetailDto> responses,
        Boolean hasNext,
        Integer currentPage,
        Integer totalPageNum
) {
    public static GetDetailResponseResponseDto from(GetDetailResponseResponse response) {
        List<ResponseDetailDto> responseDtos = response.getResponsesList().stream()
                .map(ResponseDetailDto::from)
                .toList();

        return GetDetailResponseResponseDto.builder()
                .responses(responseDtos)
                .hasNext(response.getHasNext())
                .currentPage(response.getCurrentPage())
                .totalPageNum(response.getTotalPageNum())
                .build();
    }
}