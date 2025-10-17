package com.exit.gateway.controller.search.dto.response;

import com.exit.gateway.controller.question.dto.response.question.QuestionListQueryResponseDto;
import lombok.Builder;

import java.util.List;

@Builder
public record IntegratedSearchResponseDto(
        List<QuestionListQueryResponseDto> questions,
        List<MagazineSearchItemDto> magazines,
        Boolean questionHasNext,
        Boolean magazineHasNext
) {
}
