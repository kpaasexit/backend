package com.exit.question.controller.dto.response;

import java.util.List;

public record QuestionDetailResponseDto(
        QuestionCreateResponseDto question,
        List<ResponseDetailDto> responses,
        Boolean hasNext
) {
}