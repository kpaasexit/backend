package com.exit.question.service.util.factory;

import com.exit.question.domain.Comment;

public abstract class CommentFactory {
    public abstract Comment createAndSaveComment(Long targetId, Long authorId, String content);

    public abstract void deleteComment(Long targetId, Long authorId);

    public abstract SendNotificationRequestDto createSendNotificationRequestDto(Long targetId, String deviceId);
}
