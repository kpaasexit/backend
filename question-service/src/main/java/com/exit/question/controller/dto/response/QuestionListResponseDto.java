package com.exit.question.controller.dto.response;

import java.util.List;

public record QuestionListResponseDto(
        List<QuestionListQueryResponseDto> questionList,
        boolean hasNext
) {
}
