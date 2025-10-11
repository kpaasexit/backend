package com.exit.gateway.service.question;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.question.dto.response.comment.GetCommentResponseDto;
import com.exit.gateway.controller.question.dto.response.question.CreateCommentResponseDto;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommentGrpcClient {

    @GrpcClient("question-service")
    private CommentServiceGrpc.CommentServiceBlockingStub commentServiceStub;

    public CreateCommentResponseDto createComment(CreateCommentRequest request) {
        try {
            log.debug("Sending create comment request via gRPC: targetId={}, commentType={}, writerId={}",
                    request.getTargetId(), request.getCommentType(), request.getWriterId());
            CreateCommentResponse response = commentServiceStub.createComment(request);
            log.debug("Received create comment response via gRPC: commentId={}", response.getCommentId());
            return CreateCommentResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC create comment failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public void deleteComment(DeleteCommentRequest request) {
        try {
            log.debug("Sending delete comment request via gRPC: commentId={}, userId={}, commentType={}",
                    request.getCommentId(), request.getUserId(), request.getCommentType());
            commentServiceStub.deleteComment(request);
            log.debug("Comment deleted successfully via gRPC: commentId={}", request.getCommentId());
        } catch (StatusRuntimeException e) {
            log.error("gRPC delete comment failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public GetCommentResponseDto getComment(GetCommentRequest request) {
        try {
            log.debug("Sending get comment request via gRPC: targetId={}, userId={}, targetType={}",
                    request.getTargetId(), request.getUserId(), request.getTargetType());
            GetCommentResponse response = commentServiceStub.getComment(request);
            return GetCommentResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC get comment failed: {}", e.getStatus(), e);
            throw e;
        }
    }
}
