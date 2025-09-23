package com.exit.question.controller.dto.response;

import java.util.List;

public record QuestionListResponse(
        List<QuestionListQueryResponse> questionList,
        boolean hasNext
) {
}
