package com.exit.question.controller.dto.response;

import com.exit.question.domain.response.ResponseDisclosureType;
import java.time.LocalDateTime;
import java.util.List;

public record AnswerCreateResponse(
        Long responseId,
        Long questionId,
        Long responseWriterId,
        String responseTitle,
        String responseContent,
        ResponseDisclosureType responseDisclosure,
        Boolean responseAdopt,
        List<String> imageUrls,
        List<String> referenceUrls,
        Integer likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
