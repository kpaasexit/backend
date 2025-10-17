package com.exit.gateway.service.quiz;

import com.exit.common.grpc.*;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.quiz.QuizGrpcErrorMapper;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@GrpcToRest(mapper = QuizGrpcErrorMapper.class)
public class QuizGrpcClient {

    @GrpcClient("quiz-service")
    private QuizServiceGrpc.QuizServiceBlockingStub quizServiceStub;

    public GetCategoryStatisticsResponse getCategoryStatistics(Long userId) {
        GetCategoryStatisticsRequest request = GetCategoryStatisticsRequest.newBuilder()
                .setUserId(userId)
                .build();

        log.debug("Sending get category statistics request via gRPC: userId={}", userId);
        GetCategoryStatisticsResponse response = quizServiceStub.getCategoryStatistics(request);
        log.debug("Received get category statistics response via gRPC");

        return response;
    }

    public GetQuizResponse getQuiz(Long categoryId, Long userId) {
        GetQuizRequest request = GetQuizRequest.newBuilder()
                .setCategoryId(categoryId)
                .setUserId(userId)
                .build();

        log.debug("Sending get quiz request via gRPC: categoryId={}, userId={}", categoryId, userId);
        GetQuizResponse response = quizServiceStub.getQuiz(request);
        log.debug("Received get quiz response via gRPC");

        return response;
    }

    public SubmitAnswerResponse submitAnswer(Long quizId, String answer, Long userId) {
        SubmitAnswerRequest request = SubmitAnswerRequest.newBuilder()
                .setQuizId(quizId)
                .setAnswer(answer)
                .setUserId(userId)
                .build();

        log.debug("Sending submit answer request via gRPC: quizId={}, userId={}", quizId, userId);
        SubmitAnswerResponse response = quizServiceStub.submitAnswer(request);
        log.debug("Received submit answer response via gRPC");

        return response;
    }

    public GetSolvedQuizResponse getSolvedQuiz(Long userId, List<Long> categoryIds, Integer pageNum) {
        GetSolvedQuizRequest request = GetSolvedQuizRequest.newBuilder()
                .setUserId(userId)
                .addAllCategoryId(categoryIds)
                .setPageNum(pageNum)
                .build();

        log.debug("Sending get solved quiz request via gRPC: userId={}", userId);
        GetSolvedQuizResponse response = quizServiceStub.getSolvedQuiz(request);
        log.debug("Received get solved quiz response via gRPC");

        return response;
    }

    public GetTodayQuizResponse getTodayQuiz() {
        GetTodayQuizResponse response = quizServiceStub.getTodayQuiz(Empty.getDefaultInstance());
        log.debug("Received get today quiz response via gRPC");
        return response;
    }
}