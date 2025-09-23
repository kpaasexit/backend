package com.exit.question.controller.dto.request;

import com.exit.common.grpc.AnswerCreateRequest;
import com.exit.common.grpc.UploadBytesRequest;
import com.exit.question.domain.question.QuestionDisclosureType;

import java.util.List;

public record AnswerCreateRequestDto(
        Long questionId,
        Long responseWriterId,
        String responseContent,
        Boolean responseIsAnonymous,
        List<UploadBytesRequest> images
) {
    public static AnswerCreateRequestDto from(AnswerCreateRequest request) {
        return new AnswerCreateRequestDto(
                request.getQuestionId(),
                request.getResponseWriterId(),
                request.getResponseContent(),
                request.getResponseIsAnonymous(),
                request.getImagesList()
        );
    }
}
