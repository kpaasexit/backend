package com.exit.gateway.controller.question;

import com.exit.common.grpc.*;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.QuestionSuccessCode;
import com.exit.gateway.controller.question.dto.request.question.QuestionCreateRequestDto;
import com.exit.gateway.controller.question.dto.request.question.QuestionReportRequestDto;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.question.QuestionGrpcClient;
import com.exit.gateway.service.question.QuestionRequestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

    private final QuestionGrpcClient questionGrpcClient;
    private final QuestionRequestMapper questionRequestMapper;

    @PostMapping(consumes = "multipart/form-data")
    public SuccessResponse<QuestionCreateResponseDto> createQuestion(
            @Valid @ModelAttribute QuestionCreateRequestDto request
    ) {
        log.info("Question create request received");
        QuestionCreateRequest grpcRequest = questionRequestMapper.toGrpcQuestionCreateRequest(request);
        QuestionCreateResponseDto response = questionGrpcClient.createQuestion(grpcRequest);
        return SuccessResponse.of(QuestionSuccessCode.QUESTION_CREATE_SUCCESS, response);
    }

    @GetMapping
    public SuccessResponse<QuestionListResponseDto> getQuestionList(
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false", required = false) boolean isAdopted
    ) {
        log.info("Question list request received");

        if (categoryIds == null || categoryIds.isEmpty()) {
            categoryIds = new ArrayList<>(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L));
        }

        QuestionListRequest.Builder requestBuilder = QuestionListRequest.newBuilder()
                .addAllCategoryIds(categoryIds)
                .setKeyword(keyword != null ? keyword : "")
                .setPage(page)
                .setSize(size)
                .setIsAdopted(isAdopted);

        QuestionListResponseDto response = questionGrpcClient.getQuestionList(requestBuilder.build());
        return SuccessResponse.of(QuestionSuccessCode.QUESTION_LIST_SUCCESS, response);
    }

    @GetMapping("/{questionId}")
    public SuccessResponse<QuestionDetailResponseDto> getQuestionDetail(
            @PathVariable Long questionId,
            @LoginUser Long userId
    ) {
        log.info("Question detail request received for questionId: {}", questionId);
        QuestionDetailRequest request = QuestionDetailRequest.newBuilder()
                .setQuestionId(questionId)
                .setUserId(userId)
                .build();
        QuestionDetailResponseDto response = questionGrpcClient.getQuestionDetail(request);
        return SuccessResponse.of(QuestionSuccessCode.QUESTION_DETAIL_SUCCESS, response);
    }

    @PostMapping("/{questionId}/report")
    public SuccessResponse<QuestionReportResponseDto> reportQuestion(
            @PathVariable Long questionId,
            @LoginUser Long userId,
            @Valid @RequestBody QuestionReportRequestDto request
    ) {
        log.info("Question report request received for questionId: {}", questionId);
        QuestionReportRequest grpcRequest = questionRequestMapper.toGrpcQuestionReportRequest(questionId, userId, request);
        QuestionReportResponseDto response = questionGrpcClient.reportQuestion(grpcRequest);
        return SuccessResponse.of(QuestionSuccessCode.QUESTION_REPORT_SUCCESS, response);
    }

    @GetMapping("/categories/recommend")
    public SuccessResponse<CategoryRecommendationResponseDto> recommendCategory(
            @RequestParam String title
    ) {
        log.info("Category recommend request received for title: {}", title);
        CategoryRecommendRequest request = CategoryRecommendRequest.newBuilder()
                .setTitle(title)
                .build();
        CategoryRecommendationResponseDto response = questionGrpcClient.recommendCategory(request);
        return SuccessResponse.of(QuestionSuccessCode.CATEGORY_RECOMMEND_SUCCESS, response);
    }

    @GetMapping("/similar")
    public SuccessResponse<SimilarQuestionResponseDto> getSimilarQuestion(
            @RequestParam String title,
            @RequestParam String content
    ) {
        log.info("Similar question request received for title: {}", title);
        SimilarQuestionRequest request = SimilarQuestionRequest.newBuilder()
                .setTitle(title)
                .setContent(content)
                .build();
        SimilarQuestionResponseDto response = questionGrpcClient.getSimilarQuestion(request);
        return SuccessResponse.of(QuestionSuccessCode.SIMILAR_QUESTION_SUCCESS, response);
    }

    @GetMapping("/popular-post")
    public SuccessResponse<GetPopularPostResponseDto> getPopularPost() {
        log.info("Get popular post");
        return SuccessResponse.of(QuestionSuccessCode.GET_POPULAR_POST,
                questionGrpcClient.getPopularPost());
    }

    @GetMapping("/my")
    public SuccessResponse<GetMyQuestionResponseDto> getMyQuestion(
            @LoginUser Long userId,
            @RequestParam Integer pageNum
    ) {
        log.info("Get my question for userId: {}, pageNum: {}", userId, pageNum);
        return SuccessResponse.of(QuestionSuccessCode.GET_MY_QUESTION_SUCCESS,
                questionGrpcClient.getMyQuestion(userId, pageNum - 1));
    }
}