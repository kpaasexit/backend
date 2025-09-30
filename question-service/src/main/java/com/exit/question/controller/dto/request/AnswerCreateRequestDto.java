package com.exit.question.controller.dto.request;

import com.exit.common.grpc.AnswerCreateRequest;
import com.exit.common.grpc.UploadBytesRequest;

import java.util.List;

public record AnswerCreateRequestDto(
        Long questionId,
        Long responseWriterId,
        String responseContent,
        Boolean responseIsAnonymous,
        List<UploadBytesRequest> images,
        String deviceId
) {
    public static AnswerCreateRequestDto from(AnswerCreateRequest request) {
        return new AnswerCreateRequestDto(
                request.getQuestionId(),
                request.getResponseWriterId(),
                request.getResponseContent(),
                request.getResponseIsAnonymous(),
                request.getImagesList(),
                request.getDeviceId()
        );
    }
}
