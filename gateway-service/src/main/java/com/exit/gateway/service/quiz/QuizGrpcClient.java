package com.exit.gateway.service.quiz;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.quiz.dto.request.quiz.ReportQuizRequestDto;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGrpcClient {

    @GrpcClient("quiz-service")
    private QuizServiceGrpc.QuizServiceBlockingStub quizServiceStub;

    public GetCategoryStatisticsResponse getCategoryStatistics(Long userId) {
        try {
            GetCategoryStatisticsRequest request = GetCategoryStatisticsRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            log.debug("Sending get category statistics request via gRPC: userId={}", userId);
            GetCategoryStatisticsResponse response = quizServiceStub.getCategoryStatistics(request);
            log.debug("Received get category statistics response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC get category statistics failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public GetQuizResponse getQuiz(Long categoryId, Long userId) {
        try {
            GetQuizRequest request = GetQuizRequest.newBuilder()
                    .setCategoryId(categoryId)
                    .setUserId(userId)
                    .build();

            log.debug("Sending get quiz request via gRPC: categoryId={}, userId={}", categoryId, userId);
            GetQuizResponse response = quizServiceStub.getQuiz(request);
            log.debug("Received get quiz response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC get quiz failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public SubmitAnswerResponse submitAnswer(Long quizId, String answer, Long userId) {
        try {
            SubmitAnswerRequest request = SubmitAnswerRequest.newBuilder()
                    .setQuizId(quizId)
                    .setAnswer(answer)
                    .setUserId(userId)
                    .build();

            log.debug("Sending submit answer request via gRPC: quizId={}, userId={}", quizId, userId);
            SubmitAnswerResponse response = quizServiceStub.submitAnswer(request);
            log.debug("Received submit answer response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC submit answer failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public ReportQuizResponse reportQuiz(Long quizId, Long userId, ReportQuizRequestDto requestDto) {
        try {
            ReportQuizRequest request = ReportQuizRequest.newBuilder()
                    .setQuizId(quizId)
                    .setQuizReportTitle(requestDto.title())
                    .setQuizReportContent(requestDto.content())
                    .setUserId(userId)
                    .build();

            log.debug("Sending report quiz request via gRPC: quizId={}, userId={}", quizId, userId);
            ReportQuizResponse response = quizServiceStub.reportQuiz(request);
            log.debug("Received report quiz response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC report quiz failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public GetSolvedQuizResponse getSolvedQuiz(Long userId, List<Long> categoryIds, Integer pageNum) {
        try {
            GetSolvedQuizRequest request = GetSolvedQuizRequest.newBuilder()
                    .setUserId(userId)
                    .addAllCategoryId(categoryIds)
                    .setPageNum(pageNum)
                    .build();

            log.debug("Sending get solved quiz request via gRPC: userId={}", userId);
            GetSolvedQuizResponse response = quizServiceStub.getSolvedQuiz(request);
            log.debug("Received get solved quiz response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC get solved quiz failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public GetQuizResponse resolveQuiz(Long quizId) {
        try {
            ResolveQuizRequest request = ResolveQuizRequest.newBuilder()
                    .setQuizId(quizId)
                    .build();

            log.debug("Sending resolve quiz request via gRPC: quizId={}", quizId);
            GetQuizResponse response = quizServiceStub.resolveQuiz(request);
            log.debug("Received resolve quiz response via gRPC");

            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC resolve quiz failed: {}", e.getStatus(), e);
            throw e;
        }
    }
}