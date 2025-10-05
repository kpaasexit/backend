package com.exit.gateway.controller.quiz;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.*;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.QuizErrorCode;
import com.exit.common.response.success.QuizSuccessCode;
import com.exit.gateway.controller.quiz.dto.request.quiz.ReportQuizRequestDto;
import com.exit.gateway.controller.quiz.dto.response.quiz.*;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.quiz.QuizGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
        try {
            log.info("Get category statistics request received for userId: {}", userId);
            GetCategoryStatisticsResponse response = quizGrpcClient.getCategoryStatistics(userId);
            GetCategoryStatisticsResponseDto.from(response);
            return SuccessResponse.of(QuizSuccessCode.GET_CATEGORY_STATISTICS_SUCCESS,
                    GetCategoryStatisticsResponseDto.from(response));
        } catch (StatusRuntimeException e) {
            log.error("Get category statistics failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(QuizErrorCode.GET_CATEGORY_STATISTICS_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Get category statistics failed", e);
            throw new RestApiException(QuizErrorCode.GET_CATEGORY_STATISTICS_FAIL);
        }
    }

    @GetMapping("/{categoryId}")
    public SuccessResponse<GetQuizResponseDto> getQuiz(@PathVariable Long categoryId, @LoginUser Long userId) {
        try {
            log.info("Get quiz request received for categoryId: {}, userId: {}", categoryId, userId);
            GetQuizResponse response = quizGrpcClient.getQuiz(categoryId, userId);

            return SuccessResponse.of(QuizSuccessCode.GET_QUIZ_SUCCESS,
                    GetQuizResponseDto.from(response));
        } catch (StatusRuntimeException e) {
            log.error("Get quiz failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(QuizErrorCode.GET_QUIZ_FAIL, errorMessage);

        } catch (Exception e) {
            log.error("Get quiz failed", e);
            throw new RestApiException(QuizErrorCode.GET_QUIZ_FAIL);
        }
    }

    @PostMapping("/{quizId}/submit")
    public SuccessResponse<SubmitAnswerResponseDto> submitAnswer(
            @PathVariable Long quizId,
            @RequestParam("answer") String answer,
            @LoginUser Long userId) {
        try {
            log.info("Submit answer request received for quizId: {}, userId: {}, answer:{}", quizId, userId, answer);
            SubmitAnswerResponse response = quizGrpcClient.submitAnswer(quizId, answer, userId);

            return SuccessResponse.of(QuizSuccessCode.SUBMIT_ANSWER_SUCCESS,
                    SubmitAnswerResponseDto.from(response));
        } catch (StatusRuntimeException e) {
            log.error("Submit answer failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(QuizErrorCode.SUBMIT_ANSWER_FAIL, errorMessage);

        } catch (Exception e) {
            log.error("Submit answer failed", e);
            throw new RestApiException(QuizErrorCode.SUBMIT_ANSWER_FAIL);
        }
    }

    @PostMapping("/{quizId}/report")
    public SuccessResponse<ReportQuizResponseDto> reportQuiz(
            @PathVariable Long quizId,
            @RequestBody ReportQuizRequestDto requestDto,
            @LoginUser Long userId) {
        try {
            log.info("Report quiz request received for quizId: {}, userId: {}", quizId, userId);
            ReportQuizResponse response = quizGrpcClient.reportQuiz(quizId, userId, requestDto);

            return SuccessResponse.of(QuizSuccessCode.REPORT_QUIZ_SUCCESS,
                    ReportQuizResponseDto.from(response));
        } catch (StatusRuntimeException e) {
            log.error("Report quiz failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(QuizErrorCode.REPORT_QUIZ_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Report quiz failed", e);
            throw new RestApiException(QuizErrorCode.REPORT_QUIZ_FAIL);
        }
    }

    @GetMapping("/solved")
    public SuccessResponse<GetSolvedQuizResponseDto> getSolvedQuiz(
            @LoginUser Long userId,
            @RequestParam List<Long> categoryIds,
            @RequestParam Integer pageNum
    ) {
        try {
            log.info("Get solved quiz request received for userId: {}", userId);
            GetSolvedQuizResponse response = quizGrpcClient.getSolvedQuiz(userId, categoryIds, pageNum - 1);

            return SuccessResponse.of(QuizSuccessCode.GET_SOLVED_QUIZ_SUCCESS,
                    GetSolvedQuizResponseDto.from(response));
        } catch (StatusRuntimeException e) {
            log.error("Get solved quiz failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(QuizErrorCode.GET_SOLVED_QUIZ_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Get solved quiz failed", e);
            throw new RestApiException(QuizErrorCode.GET_SOLVED_QUIZ_FAIL);
        }
    }

    @GetMapping("/{quizId}/resolve")
    public SuccessResponse<GetQuizResponseDto> resolveQuiz(@PathVariable Long quizId) {
        try {
            log.info("Resolve quiz request received for quizId: {}", quizId);
            GetQuizResponse response = quizGrpcClient.resolveQuiz(quizId);

            return SuccessResponse.of(QuizSuccessCode.RESOLVE_QUIZ_SUCCESS,
                    GetQuizResponseDto.from(response));
        } catch (StatusRuntimeException e) {
            log.error("Resolve quiz failed via gRPC: {}", e.getStatus(), e);
            String errorMessage = getGrpcErrorMessage(e);
            throw new RestApiException(QuizErrorCode.RESOLVE_QUIZ_FAIL, errorMessage);
        } catch (Exception e) {
            log.error("Resolve quiz failed", e);
            throw new RestApiException(QuizErrorCode.RESOLVE_QUIZ_FAIL);
        }
    }

    private String getGrpcErrorMessage(StatusRuntimeException e) {
        Status status = e.getStatus();
        switch (status.getCode()) {
            case INVALID_ARGUMENT:
                return "잘못된 요청입니다.";
            case UNAUTHENTICATED:
                return "인증에 실패했습니다.";
            case PERMISSION_DENIED:
                return "권한이 없습니다.";
            case NOT_FOUND:
                return "퀴즈를 찾을 수 없습니다.";
            default:
                return status.getDescription() != null ? status.getDescription() : "서버 오류가 발생했습니다.";
        }
    }
}
