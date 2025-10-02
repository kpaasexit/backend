package com.exit.question.service.grpc;

import com.exit.common.grpc.CommentServiceGrpc;
import com.exit.common.grpc.CreateCommentResponse;
import com.exit.question.service.CommentService;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class CommentGrpcService extends CommentServiceGrpc.CommentServiceImplBase {

    private final CommentService commentService;

    @Override
    public void createComment(com.exit.common.grpc.CreateCommentRequest request,
                              StreamObserver<CreateCommentResponse> responseObserver) {
        try {
            log.info("create comment request received: {}", request.getCommentType());
            CreateCommentResponse response = commentService.createComment(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("create comment request fail", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("댓글 생성 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }


    @Override
    public void deleteComment(com.exit.common.grpc.DeleteCommentRequest request,
                              StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            log.info("delete comment request received : {}", request.getCommentId());
            commentService.deleteComment(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("delete comment request fail", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("댓글 삭제 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }
}
