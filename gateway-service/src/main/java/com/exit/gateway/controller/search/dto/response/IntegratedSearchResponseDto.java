package com.exit.gateway.controller.search.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record IntegratedSearchResponseDto(
        List<QuestionSearchItemDto> questions,
        List<MagazineSearchItemDto> magazines,
        Integer questionTotalCount,
        Integer magazineTotalCount,
        Boolean questionHasNext,
        Boolean magazineHasNext
) {
}
