package com.exit.question.service.grpc;

import com.exit.common.grpc.CategoryRecommendationResponse;
import com.exit.common.grpc.DeleteQuestionRequest;
import com.exit.common.grpc.GetMyQuestionRequest;
import com.exit.common.grpc.GetMyQuestionResponse;
import com.exit.common.grpc.GetPopularPostResponse;
import com.exit.common.grpc.QuestionCreateResponse;
import com.exit.common.grpc.QuestionDetailResponse;
import com.exit.common.grpc.QuestionListResponse;
import com.exit.common.grpc.QuestionReportResponse;
import com.exit.common.grpc.QuestionServiceGrpc;
import com.exit.common.grpc.SimilarQuestionResponse;
import com.exit.common.grpc.UpdateQuestionRequest;
import com.exit.question.service.QuestionService;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class QuestionGrpcService extends QuestionServiceGrpc.QuestionServiceImplBase {

    private final QuestionService questionService;

    @Override
    public void questionCreate(com.exit.common.grpc.QuestionCreateRequest request,
                               StreamObserver<com.exit.common.grpc.QuestionCreateResponse> responseObserver) {
        log.info("Question create request received: {}", request.getQuestionTitle());
        QuestionCreateResponse response = questionService.createQuestion(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void questionReport(com.exit.common.grpc.QuestionReportRequest request,
                               StreamObserver<com.exit.common.grpc.QuestionReportResponse> responseObserver) {
        log.info("Question report request received for question ID: {}", request.getQuestionId());
        QuestionReportResponse response = questionService.questionReport(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void questionList(com.exit.common.grpc.QuestionListRequest request,
                             StreamObserver<com.exit.common.grpc.QuestionListResponse> responseObserver) {
        log.info("Question list request received");
        QuestionListResponse response = questionService.questionList(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void categoryRecommend(com.exit.common.grpc.CategoryRecommendRequest request,
                                  StreamObserver<com.exit.common.grpc.CategoryRecommendationResponse> responseObserver) {
        log.info("Category recommend request received for title: {}", request.getTitle());

        CategoryRecommendationResponse response = questionService.categoryRecommend(request.getTitle());

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void similarQuestion(com.exit.common.grpc.SimilarQuestionRequest request,
                                StreamObserver<com.exit.common.grpc.SimilarQuestionResponse> responseObserver) {
        log.info("Similar question request received for title: {}", request.getTitle());
        SimilarQuestionResponse response = questionService.similarQuestion(request.getTitle(), request.getContent());

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void getQuestionDetail(com.exit.common.grpc.QuestionDetailRequest request,
                                  StreamObserver<com.exit.common.grpc.QuestionDetailResponse> responseObserver) {
        log.info("Question detail request received for question ID: {}", request.getQuestionId());
        QuestionDetailResponse response = questionService.getQuestionDetail(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getPopularPost(Empty request,
                               StreamObserver<com.exit.common.grpc.GetPopularPostResponse> responseObserver) {
        log.info("Get popular post request received");

        GetPopularPostResponse response = questionService.getPopularPost();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getMyQuestion(GetMyQuestionRequest request,
                              StreamObserver<com.exit.common.grpc.GetMyQuestionResponse> responseObserver) {
        log.info("Get my question request received");
        GetMyQuestionResponse response = questionService.getMyQuestion(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void updateQuestion(UpdateQuestionRequest request,
                               StreamObserver<com.exit.common.grpc.QuestionCreateResponse> responseObserver) {
        log.info("Update question request received");
        QuestionCreateResponse response = questionService.updateQuestion(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteQuestion(DeleteQuestionRequest request,
                               StreamObserver<com.google.protobuf.Empty> responseObserver) {
        log.info("Update question request received");
        questionService.deleteQuestion(request);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}