package com.exit.question.controller.dto.request;

import com.exit.common.grpc.AnswerAdoptRequest;

public record AnswerAdoptRequestDto(
        Long responseId,
        String deviceId
) {
    public static AnswerAdoptRequestDto from(AnswerAdoptRequest request) {
        return new AnswerAdoptRequestDto(request.getResponseId(), request.getDeviceId());
    }
}
