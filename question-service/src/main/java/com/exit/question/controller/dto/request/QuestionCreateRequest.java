package com.exit.question.controller.dto.request;

import com.exit.question.domain.question.QuestionAnswerType;
import com.exit.question.domain.question.QuestionCategoryType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record QuestionCreateRequest(
        Long questionCategoryId,
        String questionTitle,
        String questionContent,
        QuestionCategoryType questionCategory,
        Boolean questionUrgency,
        QuestionAnswerType questionAnswerType,
        List<String> hashTags,
        List<MultipartFile> images
) {
}
