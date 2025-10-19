package com.exit.gateway.controller.search.dto.response;

import com.exit.gateway.controller.magazine.dto.response.MagazineItemDto;
import com.exit.gateway.controller.magazine.dto.response.MagazineListItemDto;
import com.exit.gateway.controller.question.dto.response.question.QuestionListQueryResponseDto;
import lombok.Builder;

import java.util.List;

@Builder
public record IntegratedSearchResponseDto(
        List<QuestionListQueryResponseDto> questions,
        List<MagazineListItemDto> magazines,
        Boolean questionHasNext,
        Boolean magazineHasNext
) {
}
