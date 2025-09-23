package com.exit.question.controller.dto.request;

import com.exit.common.grpc.UploadBytesRequest;
import com.exit.question.domain.question.QuestionDisclosureType;

import java.util.List;

public record AnswerCreateRequest(
        Long questionId,
        String responseTitle,
        String responseContent,
        QuestionDisclosureType responseDisclosure,
        Boolean responseIsAnonymous,
        List<UploadBytesRequest> images,
        List<String> referenceUrls
) {
}
