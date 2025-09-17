package com.exit.question.controller.dto.request;

import com.exit.question.domain.question.QuestionDisclosureType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record AnswerCreateRequest(
        Long questionId,
        String responseTitle,
        String responseContent,
        QuestionDisclosureType responseDisclosure,
        Boolean responseIsAnonymous,
        List<MultipartFile> images,
        List<String> referenceUrls
) {
}
