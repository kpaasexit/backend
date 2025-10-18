package com.exit.gateway.controller.quiz;

import com.exit.common.grpc.*;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.QuizSuccessCode;
import com.exit.gateway.controller.quiz.dto.response.quiz.*;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.quiz.QuizGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
@Slf4j
public class QuizController {
    private final QuizGrpcClient quizGrpcClient;

    @GetMapping("/categories/statistics")
    public SuccessResponse<List<GetCategoryStatisticsResponseDto>> getCategoryStatistics(@LoginUser Long userId) {
        log.info("Get category statistics request received for userId: {}", userId);
        GetCategoryStatisticsResponse response = quizGrpcClient.getCategoryStatistics(userId);
        GetCategoryStatisticsResponseDto.from(response);
        return SuccessResponse.of(QuizSuccessCode.GET_CATEGORY_STATISTICS_SUCCESS,
                GetCategoryStatisticsResponseDto.from(response));
    }

    @GetMapping("/category/{categoryId}")
    public SuccessResponse<GetQuizResponseDto> getQuizByCategory(@PathVariable Long categoryId, @LoginUser Long userId) {
        log.info("Get quiz request received for categoryId: {}, userId: {}", categoryId, userId);
        GetQuizResponse response = quizGrpcClient.getQuizByCategory(categoryId, userId);

        return SuccessResponse.of(QuizSuccessCode.GET_QUIZ_SUCCESS,
                GetQuizResponseDto.from(response));
    }

    @PostMapping("/{quizId}/submit")
    public SuccessResponse<SubmitAnswerResponseDto> submitAnswer(
            @PathVariable Long quizId,
            @RequestParam("answer") String answer,
            @LoginUser Long userId) {
        log.info("Submit answer request received for quizId: {}, userId: {}, answer:{}", quizId, userId, answer);
        SubmitAnswerResponse response = quizGrpcClient.submitAnswer(quizId, answer, userId);

        return SuccessResponse.of(QuizSuccessCode.SUBMIT_ANSWER_SUCCESS,
                SubmitAnswerResponseDto.from(response));
    }

    @GetMapping("/solved")
    public SuccessResponse<GetSolvedQuizResponseDto> getSolvedQuiz(
            @LoginUser Long userId,
            @RequestParam List<Long> categoryIds,
            @RequestParam Integer pageNum
    ) {
        log.info("Get solved quiz request received for userId: {}", userId);
        GetSolvedQuizResponse response = quizGrpcClient.getSolvedQuiz(userId, categoryIds, pageNum - 1);

        return SuccessResponse.of(QuizSuccessCode.GET_SOLVED_QUIZ_SUCCESS,
                GetSolvedQuizResponseDto.from(response));
    }

    @GetMapping("/today")
    public SuccessResponse<GetTodayQuizResponseDto> getTodayQuiz() {
        log.info("Resolve get today quiz received");
        GetTodayQuizResponse response = quizGrpcClient.getTodayQuiz();

        return SuccessResponse.of(QuizSuccessCode.GET_TODAY_QUIZ,
                GetTodayQuizResponseDto.from(response));
    }

    @GetMapping("/{quizId}")
    public SuccessResponse<GetQuizResponseDto> getQuizById(@PathVariable Long quizId, @LoginUser Long userId) {
        log.info("Get quiz request received for quizId: {}, userId: {}", quizId, userId);
        GetQuizResponse response = quizGrpcClient.getQuizById(quizId, userId);

        return SuccessResponse.of(QuizSuccessCode.GET_QUIZ_SUCCESS,
                GetQuizResponseDto.from(response));
    }
}
