package com.exit.question.service;

import com.exit.question.grpc.*;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class QuestionGrpcService extends QuestionServiceGrpc.QuestionServiceImplBase {

    private final QuestionService questionService;

    @Override
    public void questionCreate(com.exit.question.grpc.QuestionCreateRequest request,
                              StreamObserver<com.exit.question.grpc.QuestionCreateResponse> responseObserver) {
        try {
            log.info("Question create request received: {}", request.getQuestionTitle());

            // TODO: 현재는 기본 응답만 반환. 실제 구현에서는 QuestionService와 연동 필요
            com.exit.question.grpc.QuestionCreateResponse response = com.exit.question.grpc.QuestionCreateResponse.newBuilder()
                    .setQuestionId(1L)
                    .setQuestionTitle(request.getQuestionTitle())
                    .setQuestionContent(request.getQuestionContent())
                    .setQuestionCategory(request.getQuestionCategory())
                    .setQuestionUrgency(request.getQuestionUrgency())
                    .setQuestionAnswerType(request.getQuestionAnswerType())
                    .setQuestionDisclosureType(request.getQuestionDisclosureType())
                    .setQuestionWriterId(request.getQuestionWriterId())
                    .setCreatedAt(toTimestamp(LocalDateTime.now()))
                    .build();

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
    public void answerAdopt(com.exit.question.grpc.AnswerAdoptRequest request,
                           StreamObserver<com.exit.question.grpc.AnswerAdoptResponse> responseObserver) {
        try {
            log.info("Answer adopt request received for response ID: {}", request.getResponseId());

            com.exit.question.grpc.AnswerAdoptResponse response = com.exit.question.grpc.AnswerAdoptResponse.newBuilder()
                    .setResponseId(request.getResponseId())
                    .setResponseAdopt(true)
                    .setUpdatedAt(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Answer adopt failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 채택 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void answerCreate(com.exit.question.grpc.AnswerCreateRequest request,
                            StreamObserver<com.exit.question.grpc.AnswerCreateResponse> responseObserver) {
        try {
            log.info("Answer create request received for question ID: {}", request.getQuestionId());

            com.exit.question.grpc.AnswerCreateResponse response = com.exit.question.grpc.AnswerCreateResponse.newBuilder()
                    .setResponseId(1L)
                    .setQuestionId(request.getQuestionId())
                    .setResponseContent(request.getResponseContent())
                    .setResponseDisclosureType(request.getResponseDisclosureType())
                    .setResponseWriterId(request.getResponseWriterId())
                    .setCreatedAt(toTimestamp(LocalDateTime.now()))
                    .build();

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
    public void answerRecommend(com.exit.question.grpc.AnswerRecommendRequest request,
                               StreamObserver<com.exit.question.grpc.AnswerRecommendResponse> responseObserver) {
        try {
            log.info("Answer recommend request received for response ID: {}", request.getResponseId());

            com.exit.question.grpc.AnswerRecommendResponse response = com.exit.question.grpc.AnswerRecommendResponse.newBuilder()
                    .setCount(1)
                    .setIsRecommended(true)
                    .build();

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
    public void questionReport(com.exit.question.grpc.QuestionReportRequest request,
                              StreamObserver<com.exit.question.grpc.QuestionReportResponse> responseObserver) {
        try {
            log.info("Question report request received for question ID: {}", request.getQuestionId());

            com.exit.question.grpc.QuestionReportResponse response = com.exit.question.grpc.QuestionReportResponse.newBuilder()
                    .setQuestionReportId(1L)
                    .setQuestionId(request.getQuestionId())
                    .setQuestionReportTitle(request.getQuestionReportTitle())
                    .setQuestionReportContent(request.getQuestionReportContent())
                    .setQuestionReportWriterId(1L)
                    .setCreatedAt(toTimestamp(LocalDateTime.now()))
                    .setUpdatedAt(toTimestamp(LocalDateTime.now()))
                    .build();

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
    public void answerReport(com.exit.question.grpc.AnswerReportRequest request,
                            StreamObserver<com.exit.question.grpc.AnswerReportResponse> responseObserver) {
        try {
            log.info("Answer report request received for response ID: {}", request.getResponseId());

            com.exit.question.grpc.AnswerReportResponse response = com.exit.question.grpc.AnswerReportResponse.newBuilder()
                    .setResponseReportId(1L)
                    .setResponseId(request.getResponseId())
                    .setResponseReportTitle(request.getResponseReportTitle())
                    .setResponseReportContent(request.getResponseReportContent())
                    .setResponseReportWriterId(1L)
                    .setCreatedAt(toTimestamp(LocalDateTime.now()))
                    .setUpdatedAt(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Answer report failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 신고 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    @Override
    public void questionList(com.exit.question.grpc.QuestionListRequest request,
                            StreamObserver<com.exit.question.grpc.QuestionListResponse> responseObserver) {
        try {
            log.info("Question list request received");

            com.exit.question.grpc.QuestionListResponse response = com.exit.question.grpc.QuestionListResponse.newBuilder()
                    .setHasNext(false)
                    .build();

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
    public void categoryRecommend(com.exit.question.grpc.CategoryRecommendRequest request,
                                 StreamObserver<com.exit.question.grpc.CategoryRecommendationResponse> responseObserver) {
        try {
            log.info("Category recommend request received for title: {}", request.getTitle());

            com.exit.question.grpc.CategoryRecommendationResponse response = com.exit.question.grpc.CategoryRecommendationResponse.newBuilder()
                    .setCategoryId(1L)
                    .setCategoryName("일반")
                    .setCategoryDescription("일반적인 질문")
                    .build();

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
    public void similarQuestion(com.exit.question.grpc.SimilarQuestionRequest request,
                               StreamObserver<com.exit.question.grpc.SimilarQuestionResponse> responseObserver) {
        try {
            log.info("Similar question request received for title: {}", request.getTitle());

            com.exit.question.grpc.SimilarQuestionResponse response = com.exit.question.grpc.SimilarQuestionResponse.newBuilder()
                    .setQuestionId(1L)
                    .setQuestionTitle("유사한 질문 제목")
                    .setQuestionContent("유사한 질문 내용")
                    .setQuestionCategory(1L)
                    .setQuestionUrgency("LOW")
                    .setQuestionAnswerType("MULTIPLE")
                    .setQuestionAnswerAdopt(false)
                    .setCreatedAt(toTimestamp(LocalDateTime.now()))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Similar question failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("유사 질문 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}