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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {
    private final QuizAttemptsRepository quizAttemptsRepository;
    private final QuizRepository quizRepository;

    @Transactional(readOnly = true)
    public GetCategoryStatisticsResponse getCategoryStatistics(GetCategoryStatisticsRequest request) {
        try {
            List<CategoryStat> categoryStats = quizRepository.getQuizByCategoryId(request.getUserId())
                    .stream()
                    .map(dto -> {
                        int totalCountByCategory = dto.quizTotalCount();
                        int solvedCountByCategory = dto.quizSolvedCount();
                        return CategoryStat.newBuilder()
                            .setCategoryId(dto.categoryId())
                            .setCategoryQuizNum(totalCountByCategory)
                            .setCategorySolvedNum(solvedCountByCategory)
                            .setCanSolveQuiz(totalCountByCategory - solvedCountByCategory > 0)
                            .build();
                    })
                    .toList();

            return GetCategoryStatisticsResponse.newBuilder()
                    .addAllCategoryStat(categoryStats)
                    .build();
        } catch (Exception e) {
            log.error("Get category statistics failed for userId: {}", request.getUserId(), e);
            throw new GrpcException(GrpcQuizErrorCode.GET_CATEGORY_STATISTICS_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetQuizResponse getQuizByCategory(GetQuizByCategoryRequest request) {
        try {
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
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuizErrorCode.GET_QUIZ_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            throw new GrpcException(GrpcQuizErrorCode.GET_QUIZ_FAILED, e.getMessage());
        }
    }

    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {
        try {
            Quiz quiz = quizRepository.findQuizById(request.getQuizId())
                    .orElseThrow(() -> new GrpcException(GrpcQuizErrorCode.NOT_FOUND_QUIZ));

            saveQuizAttempt(request.getUserId(), quiz);

            boolean hasNext = quizRepository.findRandomUnsolvedByCategoryIdAndUserId(
                            quiz.getQuizCategory().getId(), request.getUserId())
                    .isPresent();
            return SubmitAnswerResponse.newBuilder()
                    .setHasNext(hasNext)
                    .build();
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuizErrorCode.SUBMIT_ANSWER_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            throw new GrpcException(GrpcQuizErrorCode.SUBMIT_ANSWER_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetSolvedQuizResponse getSolvedQuiz(GetSolvedQuizRequest request) {
        try {
            PageRequest pageRequest = PageRequest.of(request.getPageNum(), request.getSize());
            List<Short> categoryIds = request.getCategoryIdList().stream().map(Long::shortValue).toList();
            Page<GetSolvedQuiz> total = quizAttemptsRepository.findByQuizCategoryIdIn(
                    categoryIds, request.getUserId(), pageRequest);
            List<AttemptQuiz> attemptQuizzes = total.getContent()
                    .stream()
                    .map(quiz -> AttemptQuiz.newBuilder()
                            .setQuizId(quiz.quizId())
                            .setQuizTitle(quiz.quizTitle())
                            .build()
                    ).toList();

            return GetSolvedQuizResponse.newBuilder()
                    .addAllAttemptQuiz(attemptQuizzes)
                    .setHasNext(total.hasNext())
                    .setCurrentPage(total.getNumber() + 1)
                    .setTotalPageNum(total.getTotalPages())
                    .build();
        } catch (Exception e) {
            throw new GrpcException(GrpcQuizErrorCode.GET_SOLVED_QUIZ_FAILED, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public GetTodayQuizResponse getTodayQuiz() {
        try {
            long quizTotalNum = quizRepository.count();
            long todayQuizId = Math.floorMod(LocalDate.now().toString().hashCode(), (int) quizTotalNum) + 1L;
            return GetTodayQuizResponse.newBuilder()
                    .setQuizId(todayQuizId)
                    .build();
        } catch (Exception e) {
            throw new GrpcException(GrpcQuizErrorCode.GET_TODAY_QUIZ_FAILED, e.getMessage());
        }
    }

    private void saveQuizAttempt(Long userId, Quiz quiz) {
        try {
            QuizAttempts attempt = QuizAttempts.builder()
                    .quiz(quiz)
                    .userId(userId)
                    .build();
            quizAttemptsRepository.save(attempt);
        } catch (Exception e) {
            log.warn("save quiz attempt failed for userId: {}", userId, e);
        }
    }

    @Transactional(readOnly = true)
    public GetQuizResponse getQuizById(GetQuizByIdRequest request) {
        try {
            Quiz quiz = quizRepository.findById(request.getQuizId())
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
        } catch (GrpcException e) {
            throw new GrpcException(GrpcQuizErrorCode.GET_QUIZ_FAILED, e.getGrpcErrorCode().getErrorDescription());
        } catch (Exception e) {
            throw new GrpcException(GrpcQuizErrorCode.GET_QUIZ_FAILED, e.getMessage());
        }
    }
}
