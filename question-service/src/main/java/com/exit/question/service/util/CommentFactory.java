package com.exit.question.service.util;

import com.exit.question.controller.dto.request.SendNotificationRequestDto;
import com.exit.question.domain.Comment;

public abstract class CommentFactory {
    public abstract Comment createAndSaveComment(Long targetId, Long authorId, String content);

    public abstract void deleteComment(Long targetId, Long authorId);

    public abstract SendNotificationRequestDto createSendNotificationRequestDto(Long targetId, String deviceId);
}
