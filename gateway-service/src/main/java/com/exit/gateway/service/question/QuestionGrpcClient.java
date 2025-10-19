package com.exit.gateway.service.question;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.question.QuestionGrpcErrorMapper;
import com.google.protobuf.Empty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@GrpcToRest(mapper = QuestionGrpcErrorMapper.class)
public class QuestionGrpcClient {

    @GrpcClient("question-service")
    private QuestionServiceGrpc.QuestionServiceBlockingStub questionServiceStub;

    public QuestionCreateResponseDto createQuestion(QuestionCreateRequest request) {
        log.debug("Sending question create request via gRPC");
        QuestionCreateResponse response = questionServiceStub.questionCreate(request);
        log.debug("Received question create response via gRPC");
        return QuestionCreateResponseDto.from(response);
    }

    public QuestionReportResponseDto reportQuestion(QuestionReportRequest request) {
        log.debug("Sending question report request via gRPC");
        QuestionReportResponse response = questionServiceStub.questionReport(request);
        log.debug("Received question report response via gRPC");
        return QuestionReportResponseDto.from(response);
    }

    public QuestionListResponseDto getQuestionList(QuestionListRequest request) {
        log.debug("Sending question list request via gRPC");
        QuestionListResponse response = questionServiceStub.questionList(request);
        log.debug("Received question list response via gRPC");
        return QuestionListResponseDto.from(response);
    }

    public CategoryRecommendationResponseDto recommendCategory(CategoryRecommendRequest request) {
        log.debug("Sending category recommend request via gRPC");
        CategoryRecommendationResponse response = questionServiceStub.categoryRecommend(request);
        log.debug("Received category recommend response via gRPC");
        return CategoryRecommendationResponseDto.from(response);
    }

    public SimilarQuestionResponseDto getSimilarQuestion(SimilarQuestionRequest request) {
        log.debug("Sending similar question request via gRPC");
        SimilarQuestionResponse response = questionServiceStub.similarQuestion(request);
        log.debug("Received similar question response via gRPC");
        return SimilarQuestionResponseDto.from(response);
    }

    public QuestionDetailResponseDto getQuestionDetail(QuestionDetailRequest request) {
        log.debug("Sending question detail request via gRPC for questionId: {}", request.getQuestionId());
        QuestionDetailResponse response = questionServiceStub.getQuestionDetail(request);
        log.debug("Received question detail response via gRPC");
        return QuestionDetailResponseDto.from(response);
    }

    public GetPopularPostResponseDto getPopularPost() {
        log.debug("Sending get Popular post response request via gRPC");
        GetPopularPostResponse response = questionServiceStub.getPopularPost(Empty.getDefaultInstance());
        log.debug("Received delete response response via gRPC");
        return GetPopularPostResponseDto.from(response);
    }

    public GetMyQuestionResponseDto getMyQuestion(Long userId, Integer pageNum) {
        log.debug("Sending get my question response request via gRPC");
        GetMyQuestionRequest request = GetMyQuestionRequest.newBuilder()
                .setPageNum(pageNum)
                .setUserId(userId)
                .build();
        GetMyQuestionResponse response = questionServiceStub.getMyQuestion(request);
        log.debug("Received my question response response via gRPC");
        return GetMyQuestionResponseDto.from(response);
    }
}