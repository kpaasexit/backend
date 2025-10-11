package com.exit.question.service;

import com.exit.common.grpc.*;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.domain.Comment;
import com.exit.question.domain.CommentType;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.client.UserGrpcClient;
import com.exit.question.service.util.factory.CommentFactory;
import com.exit.question.service.util.factory.CommentFactoryManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentFactoryManager commentFactoryManager;
    private final UserGrpcClient userGrpcClient;
    private final NotificationGrpcClient notificationGrpcClient;

    public CreateCommentResponse createComment(CreateCommentRequest request) {
        CommentFactory factory = commentFactoryManager.getFactory(CommentType.valueOf(request.getCommentType()));
        Comment comment = factory.createAndSaveComment(request.getTargetId(), request.getWriterId(), request.getContent());
        String authorName = userGrpcClient.getUserName(comment.getAuthorId());

        SendNotificationRequest sendNotificationRequest = factory.createSendNotificationRequest(request.getTargetId(), request.getDeviceId());
        notificationGrpcClient.sendNotification(sendNotificationRequest);

        return CreateCommentResponse.newBuilder()
                .setCommentId(comment.getCommentId())
                .setContent(comment.getContent())
                .setUserName(authorName)
                .setCreatedAt(TimeStampUtil.toGrpcTimestamp(comment.getCreatedAt()))
                .build();
    }

    public void deleteComment(DeleteCommentRequest request) {
        CommentFactory factory = commentFactoryManager.getFactory(CommentType.valueOf(request.getCommentType()));
        factory.deleteComment(request.getCommentId(), request.getUserId());
    }

    public GetCommentResponse getComment(GetCommentRequest request) {
        CommentFactory factory = commentFactoryManager.getFactory(CommentType.valueOf(request.getTargetType()));
        return factory.getCommentList(request.getTargetId(), request.getUserId(), request.getPageNum());
    }
}
