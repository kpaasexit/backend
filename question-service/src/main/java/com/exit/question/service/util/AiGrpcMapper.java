package com.exit.question.service.util;

import com.exit.common.grpc.ai.SaveQuestionRequest;
import com.exit.question.domain.question.Question;
import org.springframework.stereotype.Component;

@Component
public class AiGrpcMapper {

    public SaveQuestionRequest getSaveQuestionRequest(String content, Question question) {
        return SaveQuestionRequest.newBuilder()
                .setQuestionId(question.getQuestionId())
                .setContent(content)
                .setCategoryId(question.getQuestionCategory().getQuestionCategoryId().intValue())
                .build();
    }
}
