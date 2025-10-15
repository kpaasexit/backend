package com.exit.quiz.service;

import com.exit.common.grpc.*;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class QuizGrpcService extends QuizServiceGrpc.QuizServiceImplBase {

    private final QuizService quizService;

    @Override
    public void getCategoryStatistics(com.exit.common.grpc.GetCategoryStatisticsRequest request,
                                      StreamObserver<GetCategoryStatisticsResponse> responseObserver) {
        GetCategoryStatisticsResponse response = quizService.getCategoryStatistics(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getQuiz(com.exit.common.grpc.GetQuizRequest request,
                        StreamObserver<GetQuizResponse> responseObserver) {
        GetQuizResponse response = quizService.getQuiz(request);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void submitAnswer(com.exit.common.grpc.SubmitAnswerRequest request,
                             StreamObserver<SubmitAnswerResponse> responseObserver) {
        SubmitAnswerResponse response = quizService.submitAnswer(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getSolvedQuiz(com.exit.common.grpc.GetSolvedQuizRequest request,
                              StreamObserver<GetSolvedQuizResponse> responseObserver) {
        GetSolvedQuizResponse response = quizService.getSolvedQuiz(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTodayQuiz(Empty request,
                            StreamObserver<GetTodayQuizResponse> responseObserver) {
        GetTodayQuizResponse response = quizService.getTodayQuiz();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}