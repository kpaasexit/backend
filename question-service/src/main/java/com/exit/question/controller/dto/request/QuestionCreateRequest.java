package com.exit.question.controller.dto.request;

import com.exit.question.domain.question.QuestionAnswerType;
import com.exit.question.domain.question.QuestionDisclosureType;
import lombok.Builder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record QuestionCreateRequest(
        Long questionCategoryId,
        String questionTitle,
        String questionContent,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        QuestionDisclosureType questionDisclosure,
        Boolean questionIsAnonymous,
        List<MultipartFile> images
) {
    @Builder
    public static QuestionCreateRequest from(QuestionCreateRequest questionCreateRequest) {}
}
