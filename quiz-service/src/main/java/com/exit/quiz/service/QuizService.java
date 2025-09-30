package com.exit.quiz.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.quiz.controller.dto.response.GetSolvedQuiz;
import com.exit.quiz.domain.Quiz;
import com.exit.quiz.domain.QuizAttempts;
import com.exit.quiz.domain.QuizReport;
import com.exit.quiz.domain.QuizStats;
import com.exit.quiz.domain.repository.QuizAttemptsRepository;
import com.exit.quiz.domain.repository.QuizReportRepository;
import com.exit.quiz.domain.repository.QuizRepository;
import com.exit.quiz.domain.repository.QuizStatsRepository;
import com.exit.quiz.exception.GrpcQuizErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {
    private final QuizAttemptsRepository quizAttemptsRepository;
    private final QuizReportRepository quizReportRepository;
    private final QuizRepository quizRepository;
    private final QuizStatsRepository quizStatsRepository;

    @Transactional(readOnly = true)
    public GetCategoryStatisticsResponse getCategoryStatistics(GetCategoryStatisticsRequest request) {

        List<CategoryStat> categoryStats = quizRepository.getQuizByCategoryId(request.getUserId())
                .stream()
                .map(categoryQuizCount -> CategoryStat.newBuilder()
                        .setCategoryId(categoryQuizCount.categoryId())
                        .setCategoryQuizNum(categoryQuizCount.quizTotalCount())
                        .setCategorySolvedNum(categoryQuizCount.quizSolvedCount())
                        .build())
                .toList();

        return GetCategoryStatisticsResponse.newBuilder()
                .addAllCategoryStat(categoryStats)
                .build();
    }

    @Transactional(readOnly = true)
    public GetQuizResponse getQuiz(GetQuizRequest request) {

        Quiz quiz = quizRepository.findRandomUnsolvedByCategoryIdAndUserId(
                        request.getCategoryId(), request.getUserId())
                .orElseThrow(() -> new GrpcException(GrpcQuizErrorCode.NO_AVAILABLE_QUIZ));

        return GetQuizResponse.newBuilder()
                .setQuizId(quiz.getId())
                .setQuizCategoryId(quiz.getQuizCategory().getId())
                .setQuizTitle(quiz.getTitle())
                .setQuizContent(quiz.getContent())
                .setQuizType(quiz.getType().name())
                .setQuizCorrectAnswer(quiz.getCorrectAnswer())
                .setQuizAdditionalInformation(quiz.getAdditionalInformation())
                .build();
    }

    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {

        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new GrpcException(GrpcQuizErrorCode.NOT_FOUND_QUIZ));

        boolean isCorrect = quiz.getCorrectAnswer().equals(request.getAnswer());

        QuizAttempts attempt = QuizAttempts.builder()
                .quiz(quiz)
                .userId(request.getUserId())
                .userSelectionAnswer(request.getAnswer())
                .isCorrect(isCorrect)
                .build();

        quizAttemptsRepository.save(attempt);

        QuizStats stats = quizStatsRepository.findByQuizId(quiz.getId())
                .orElse(new QuizStats(quiz, 0, 0));

        stats.incrementTotal();
        if (isCorrect) {
            stats.incrementCorrect();
        }

        quizStatsRepository.save(stats);

        return SubmitAnswerResponse.newBuilder()
                .setQuizId(quiz.getId())
                .setQuizTotalAttemptNum(stats.getTotalNumber())
                .setQuizCorrectNum(stats.getCorrectAnswerNumber())
                .setQuizCorrectPercent(stats.getCorrectRate())
                .build();
    }

    public ReportQuizResponse reportQuiz(ReportQuizRequest request) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new GrpcException(GrpcQuizErrorCode.NOT_FOUND_QUIZ));
        QuizReport quizReport = QuizReport.createQuizReport(quiz, request);
        QuizReport savedQuizReport = quizReportRepository.save(quizReport);

        return ReportQuizResponse.newBuilder()
                .setQuizReportId(savedQuizReport.getId())
                .build();
    }

    @Transactional(readOnly = true)
    public GetSolvedQuizResponse getSolvedQuiz(GetSolvedQuizRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPageNum(), 5);
        Slice<GetSolvedQuiz> slice = quizAttemptsRepository.findByQuizCategoryIdIn(
                request.getCategoryIdList(), request.getUserId(), pageRequest);
        List<AttemptQuiz> attemptQuizs = slice
                .getContent().stream()
                .map(quiz -> AttemptQuiz.newBuilder()
                        .setQuizId(quiz.quizId())
                        .setQuizTitle(quiz.quizTitle())
                        .build()
                ).toList();

        return GetSolvedQuizResponse.newBuilder()
                .addAllAttemptQuiz(attemptQuizs)
                .setHasNext(slice.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public GetQuizResponse resolveQuiz(ResolveQuizRequest request) {
        Quiz quiz = quizRepository.findQuizWithQuizCategoryByQuizId(request.getQuizId())
                .orElseThrow(() -> new GrpcException(GrpcQuizErrorCode.NO_AVAILABLE_QUIZ));

        return GetQuizResponse.newBuilder()
                .setQuizId(quiz.getId())
                .setQuizCategoryId(quiz.getQuizCategory().getId())
                .setQuizTitle(quiz.getTitle())
                .setQuizContent(quiz.getContent())
                .setQuizType(quiz.getType().name())
                .setQuizCorrectAnswer(quiz.getCorrectAnswer())
                .setQuizAdditionalInformation(quiz.getAdditionalInformation())
                .build();
    }
}
