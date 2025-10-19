package com.exit.question.service.grpc;

import com.exit.common.grpc.CommentServiceGrpc;
import com.exit.common.grpc.CreateCommentResponse;
import com.exit.common.grpc.GetCommentResponse;
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
            log.info("create comment request received: {}", request.getCommentType());
            CreateCommentResponse response = commentService.createComment(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }


    @Override
    public void deleteComment(com.exit.common.grpc.DeleteCommentRequest request,
                              StreamObserver<com.google.protobuf.Empty> responseObserver) {
            log.info("delete comment request received : {}", request.getCommentId());
            commentService.deleteComment(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
    }

    @Override
    public void getComment(com.exit.common.grpc.GetCommentRequest request,
                              StreamObserver<GetCommentResponse> responseObserver) {
            log.info("get comment request received : {}", request.getTargetId());
            GetCommentResponse response = commentService.getComment(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }
}
