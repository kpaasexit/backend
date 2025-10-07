package com.exit.question.service.util;

import com.exit.common.grpc.SendNotificationRequest;
import com.exit.question.domain.question.Question;
import org.springframework.stereotype.Component;

@Component
public class NotificationGrpcMapper {
    public SendNotificationRequest getSendNotificationRequest(String body, String type, Question question) {
        return SendNotificationRequest.newBuilder()
                .setBody(body)
                .setType(type)
                .setTargetId(question.getQuestionId())
                .setReceiverId(question.getQuestionWriterId())
                .build();
    }

    public SendNotificationRequest getSendNotificationRequest(String body, String type, Long targetId, Long writerId) {
        return SendNotificationRequest.newBuilder()
                .setBody(body)
                .setType(type)
                .setTargetId(targetId)
                .setReceiverId(writerId)
                .build();
    }
}
