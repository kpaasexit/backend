package com.exit.question.service.grpc;

import com.exit.common.grpc.*;
import com.exit.question.service.QuestionService;
import com.exit.question.service.ResponseService;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class QuestionGrpcService extends QuestionServiceGrpc.QuestionServiceImplBase {

    private final QuestionService questionService;
    private final ResponseService responseService;

    @Override
    public void questionCreate(com.exit.common.grpc.QuestionCreateRequest request,
                               StreamObserver<com.exit.common.grpc.QuestionCreateResponse> responseObserver) {
        try {
            log.info("Question create request received: {}", request.getQuestionTitle());
            QuestionCreateResponse response = questionService.createQuestion(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Question create failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("질문 생성 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void answerCreate(com.exit.common.grpc.AnswerCreateRequest request,
                             StreamObserver<com.exit.common.grpc.AnswerCreateResponse> responseObserver) {
        try {
            log.info("Answer create request received for question ID: {}", request.getQuestionId());

            AnswerCreateResponse response = responseService.answerCreate(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Answer create failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 생성 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void answerRecommend(com.exit.common.grpc.AnswerRecommendRequest request,
                                StreamObserver<com.exit.common.grpc.AnswerRecommendResponse> responseObserver) {
        try {
            log.info("Answer recommend request received for response ID: {}", request.getResponseId());
            AnswerRecommendResponse response = responseService.toggleAnswerLike(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Answer recommend failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 추천 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void questionReport(com.exit.common.grpc.QuestionReportRequest request,
                               StreamObserver<com.exit.common.grpc.QuestionReportResponse> responseObserver) {
        try {
            log.info("Question report request received for question ID: {}", request.getQuestionId());
            QuestionReportResponse response = questionService.questionReport(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Question report failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("질문 신고 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void questionList(com.exit.common.grpc.QuestionListRequest request,
                             StreamObserver<com.exit.common.grpc.QuestionListResponse> responseObserver) {
        try {
            log.info("Question list request received");
            QuestionListResponse response = questionService.questionList(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Question list failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("질문 목록 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void categoryRecommend(com.exit.common.grpc.CategoryRecommendRequest request,
                                  StreamObserver<com.exit.common.grpc.CategoryRecommendationResponse> responseObserver) {
        try {
            log.info("Category recommend request received for title: {}", request.getTitle());

            CategoryRecommendationResponse response = questionService.categoryRecommend(request.getTitle());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Category recommend failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("카테고리 추천 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void similarQuestion(com.exit.common.grpc.SimilarQuestionRequest request,
                                StreamObserver<com.exit.common.grpc.SimilarQuestionResponse> responseObserver) {
        try {
            log.info("Similar question request received for title: {}", request.getTitle());
            SimilarQuestionResponse response = questionService.similarQuestion(request.getTitle(), request.getContent());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Similar question failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("유사 질문 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void getQuestionDetail(com.exit.common.grpc.QuestionDetailRequest request,
                                  StreamObserver<com.exit.common.grpc.QuestionDetailResponse> responseObserver) {
        try {
            log.info("Question detail request received for question ID: {}", request.getQuestionId());
            QuestionDetailResponse response = questionService.getQuestionDetail(request.getQuestionId());

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Question detail failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("질문 상세 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void updateResponse(com.exit.common.grpc.UpdateResponseRequest request,
                               StreamObserver<com.exit.common.grpc.UpdateResponseResponse> responseObserver) {
        try {
            log.info("Update response request received for response id: {}", request.getResponseId());

            UpdateResponseResponse response = responseService.updateResponse(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Update response failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 수정 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteResponse(com.exit.common.grpc.DeleteResponseRequest request,
                               StreamObserver<com.google.protobuf.Empty> responseObserver) {
        try {
            log.info("Delete response request received for response ID: {}", request.getResponseId());

            responseService.deleteResponse(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Delete Response failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 삭제 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }
}