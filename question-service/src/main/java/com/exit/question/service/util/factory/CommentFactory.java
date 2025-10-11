package com.exit.question.service.util.factory;

import com.exit.common.grpc.CommentItem;
import com.exit.common.grpc.GetCommentResponse;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.question.domain.Comment;

import java.util.List;

public abstract class CommentFactory {
    public abstract Comment createAndSaveComment(Long targetId, Long authorId, String content);

    public abstract void deleteComment(Long targetId, Long authorId);

    public abstract SendNotificationRequest createSendNotificationRequest(Long targetId, String deviceId);

    public abstract GetCommentResponse getCommentList(Long targetId, Long userId, Integer pageNum);
}
