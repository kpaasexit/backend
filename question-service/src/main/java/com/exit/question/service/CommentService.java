package com.exit.question.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.domain.Comment;
import com.exit.question.domain.CommentType;
import com.exit.question.exception.GrpcCommentErrorCode;
import com.exit.question.service.client.NotificationGrpcClient;
import com.exit.question.service.client.UserGrpcClient;
import com.exit.question.service.util.factory.CommentFactory;
import com.exit.question.service.util.factory.CommentFactoryManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentFactoryManager commentFactoryManager;
    private final UserGrpcClient userGrpcClient;
    private final NotificationGrpcClient notificationGrpcClient;

    public CreateCommentResponse createComment(CreateCommentRequest request) {
        try {
            CommentFactory factory = commentFactoryManager.getFactory(CommentType.valueOf(request.getCommentType().toUpperCase()));
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
        } catch (GrpcException e) {
            throw new GrpcException(GrpcCommentErrorCode.CREATE_COMMENT_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Create comment failed for targetId: {}", request.getTargetId(), e);
            throw new GrpcException(GrpcCommentErrorCode.CREATE_COMMENT_FAILED, e.getMessage());
        }
    }

    public void deleteComment(DeleteCommentRequest request) {
        try {
            CommentFactory factory = commentFactoryManager.getFactory(CommentType.valueOf(request.getCommentType()));
            factory.deleteComment(request.getCommentId(), request.getUserId());
        } catch (GrpcException e) {
            throw new GrpcException(GrpcCommentErrorCode.DELETE_COMMENT_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            log.error("Delete comment failed for commentId: {}", request.getCommentId(), e);
            throw new GrpcException(GrpcCommentErrorCode.DELETE_COMMENT_FAILED, e.getMessage());
        }
    }

    public GetCommentResponse getComment(GetCommentRequest request) {
        try {
            CommentFactory factory = commentFactoryManager.getFactory(CommentType.valueOf(request.getTargetType()));
            return factory.getCommentList(request.getTargetId(), request.getUserId(), request.getPageNum());
        } catch (Exception e) {
            log.error("Get comment failed for targetId: {}", request.getTargetId(), e);
            throw new GrpcException(GrpcCommentErrorCode.GET_COMMENT_FAILED, e.getMessage());
        }
    }
}
