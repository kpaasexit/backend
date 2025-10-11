package com.exit.gateway.service.question;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.google.protobuf.Empty;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.coyote.Response;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionGrpcClient {

    @GrpcClient("question-service")
    private QuestionServiceGrpc.QuestionServiceBlockingStub questionServiceStub;
    @GrpcClient("question-service")
    private ResponseServiceGrpc.ResponseServiceBlockingStub responseServiceStub;


    public QuestionCreateResponseDto createQuestion(QuestionCreateRequest request) {
        try {
            log.debug("Sending question create request via gRPC");
            QuestionCreateResponse response = questionServiceStub.questionCreate(request);
            log.debug("Received question create response via gRPC");
            return QuestionCreateResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC question create failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerAdoptResponseDto adoptAnswer(AnswerAdoptRequest request) {
        try {
            log.debug("Sending answer adopt request via gRPC");
            AnswerAdoptResponse response = responseServiceStub.answerAdopt(request);
            log.debug("Received answer adopt response via gRPC");
            return AnswerAdoptResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer adopt failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerCreateResponseDto createAnswer(AnswerCreateRequest request) {
        try {
            log.debug("Sending answer create request via gRPC");
            AnswerCreateResponse response = questionServiceStub.answerCreate(request);
            log.debug("Received answer create response via gRPC");
            return AnswerCreateResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer create failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerRecommendResponseDto recommendAnswer(AnswerRecommendRequest request) {
        try {
            log.debug("Sending answer recommend request via gRPC");
            AnswerRecommendResponse response = questionServiceStub.answerRecommend(request);
            log.debug("Received answer recommend response via gRPC");
            return AnswerRecommendResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer recommend failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public QuestionReportResponseDto reportQuestion(QuestionReportRequest request) {
        try {
            log.debug("Sending question report request via gRPC");
            QuestionReportResponse response = questionServiceStub.questionReport(request);
            log.debug("Received question report response via gRPC");
            return QuestionReportResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC question report failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerReportResponseDto reportAnswer(AnswerReportRequest request) {
        try {
            log.debug("Sending answer report request via gRPC");
            AnswerReportResponse response = responseServiceStub.answerReport(request);
            log.debug("Received answer report response via gRPC");
            return AnswerReportResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer report failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public QuestionListResponseDto getQuestionList(QuestionListRequest request) {
        try {
            log.debug("Sending question list request via gRPC");
            QuestionListResponse response = questionServiceStub.questionList(request);
            log.debug("Received question list response via gRPC");
            return QuestionListResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC question list failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public CategoryRecommendationResponseDto recommendCategory(CategoryRecommendRequest request) {
        try {
            log.debug("Sending category recommend request via gRPC");
            CategoryRecommendationResponse response = questionServiceStub.categoryRecommend(request);
            log.debug("Received category recommend response via gRPC");
            return CategoryRecommendationResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC category recommend failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public SimilarQuestionResponseDto getSimilarQuestion(SimilarQuestionRequest request) {
        try {
            log.debug("Sending similar question request via gRPC");
            SimilarQuestionResponse response = questionServiceStub.similarQuestion(request);
            log.debug("Received similar question response via gRPC");
            return SimilarQuestionResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC similar question failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public QuestionDetailResponseDto getQuestionDetail(QuestionDetailRequest request) {
        try {
            log.debug("Sending question detail request via gRPC for questionId: {}", request.getQuestionId());
            QuestionDetailResponse response = questionServiceStub.getQuestionDetail(request);
            log.debug("Received question detail response via gRPC");
            return QuestionDetailResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC question detail failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerUpdateResponseDto updateResponse(UpdateResponseRequest request) {
        try {
            log.debug("Sending update response request via gRPC for responseId: {}", request.getResponseId());
            UpdateResponseResponse response = questionServiceStub.updateResponse(request);
            log.debug("Received update response response via gRPC");
            return new AnswerUpdateResponseDto(response.getResponseId(), response.getContent());
        } catch (StatusRuntimeException e) {
            log.error("gRPC update response failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public void deleteResponse(DeleteResponseRequest request) {
        try {
            log.debug("Sending delete response request via gRPC for responseId: {}", request.getResponseId());
            questionServiceStub.deleteResponse(request);
            log.debug("Received delete response response via gRPC");
        } catch (StatusRuntimeException e) {
            log.error("gRPC delete response failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public GetPopularPostResponseDto getPopularPost() {
        try {
            log.debug("Sending get Popular post response request via gRPC");
            GetPopularPostResponse response = questionServiceStub.getPopularPost(Empty.getDefaultInstance());
            log.debug("Received delete response response via gRPC");
            return GetPopularPostResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC delete response failed: {}", e.getStatus(), e);
            throw e;
        }
    }
}