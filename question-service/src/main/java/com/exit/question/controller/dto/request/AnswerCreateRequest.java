package com.exit.question.controller.dto.request;

import com.exit.question.domain.response.ResponseDisclosureType;
import java.util.List;

public record AnswerCreateRequest(
        Long questionId,
        String responseTitle,
        String responseContent,
        ResponseDisclosureType responseDisclosure,
        List<String> imageUrls,
        List<String> referenceUrls
) {
}
