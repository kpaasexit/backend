package com.exit.quiz.service;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.quiz.controller.dto.response.GetSolvedQuiz;
import com.exit.quiz.domain.Quiz;
import com.exit.quiz.domain.QuizAttempts;
import com.exit.quiz.domain.QuizType;
import com.exit.quiz.domain.repository.QuizAttemptsRepository;
import com.exit.quiz.domain.repository.QuizRepository;
import com.exit.quiz.exception.GrpcQuizErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {
    private final QuizAttemptsRepository quizAttemptsRepository;
    private final QuizRepository quizRepository;

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
                        (short) request.getCategoryId(), request.getUserId())
                .orElseThrow(() -> new GrpcException(GrpcQuizErrorCode.NO_AVAILABLE_QUIZ));


        GetQuizResponse.Builder builder = GetQuizResponse.newBuilder();

        // OX, MULTIPLE 따라서 다르게
        if(quiz.getType().equals(QuizType.MULTIPLE)) {
            List<String> options = Arrays.stream(quiz.getContent().split("\n")).toList();
            builder.addAllQuizContent(options);
        }

        return builder
                .setQuizId(quiz.getId())
                .setQuizCategoryId(quiz.getQuizCategory().getId())
                .setQuizTitle(quiz.getTitle())
                .setQuizType(quiz.getType().name())
                .setQuizCorrectAnswer(quiz.getCorrectAnswer())
                .setQuizAdditionalInformation(quiz.getAdditionalInformation())
                .build();
    }

    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {

        Quiz quiz = quizRepository.findQuizById(request.getQuizId())
                .orElseThrow(() -> new GrpcException(GrpcQuizErrorCode.NOT_FOUND_QUIZ));

        QuizAttempts attempt = QuizAttempts.builder()
                .quiz(quiz)
                .userId(request.getUserId())
                .build();
        quizAttemptsRepository.save(attempt);

        boolean hasNext = quizRepository.findRandomUnsolvedByCategoryIdAndUserId(
                        quiz.getQuizCategory().getId(), request.getUserId())
                .isPresent();
        return SubmitAnswerResponse.newBuilder()
                .setHasNext(hasNext)
                .build();
    }

    @Transactional(readOnly = true)
    public GetSolvedQuizResponse getSolvedQuiz(GetSolvedQuizRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPageNum(), 5);
        List<Short> categoryIds = request.getCategoryIdList().stream().map(Long::shortValue).toList();
        Slice<GetSolvedQuiz> slice = quizAttemptsRepository.findByQuizCategoryIdIn(
                categoryIds, request.getUserId(), pageRequest);
        List<AttemptQuiz> attemptQuizzes = slice.getContent()
                .stream()
                .map(quiz -> AttemptQuiz.newBuilder()
                        .setQuizId(quiz.quizId())
                        .setQuizTitle(quiz.quizTitle())
                        .build()
                ).toList();

        return GetSolvedQuizResponse.newBuilder()
                .addAllAttemptQuiz(attemptQuizzes)
                .setHasNext(slice.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public GetTodayQuizResponse getTodayQuiz() {
        long quizTotalNum = quizRepository.count();
        long todayQuizId = Math.floorMod(LocalDate.now().toString().hashCode(), (int) quizTotalNum) + 1L;
        return GetTodayQuizResponse.newBuilder()
                .setQuizId(todayQuizId)
                .build();
    }
}
