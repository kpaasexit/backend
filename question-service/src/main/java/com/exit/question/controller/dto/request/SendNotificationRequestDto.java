package com.exit.question.controller.dto.request;

import com.exit.common.grpc.SendNotificationRequest;
import lombok.Builder;

@Builder
public record SendNotificationRequestDto(
        String body,
        String type,
        Long targetId,
        Long receiverId,
        String deviceId
) {
    public SendNotificationRequest toSendNotificationRequest() {
        return SendNotificationRequest.newBuilder()
                .setBody(body)
                .setType(type)
                .setTargetId(targetId)
                .setReceiverId(receiverId)
                .setDeviceId(deviceId)
                .build();
    }
}
