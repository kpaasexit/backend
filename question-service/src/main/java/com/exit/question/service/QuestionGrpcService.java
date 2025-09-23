package com.exit.question.service;

import com.exit.common.grpc.*;
import com.exit.question.controller.dto.request.*;
import com.exit.question.controller.dto.response.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.ArrayList;
import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.toGrpcTimestamp;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class QuestionGrpcService extends QuestionServiceGrpc.QuestionServiceImplBase {

    private final QuestionService questionService;

    @Override
    public void questionCreate(com.exit.common.grpc.QuestionCreateRequest request,
                              StreamObserver<com.exit.common.grpc.QuestionCreateResponse> responseObserver) {
        try {
            log.info("Question create request received: {}", request.getQuestionTitle());
            QuestionCreateRequestDto requestDto = QuestionCreateRequestDto.from(request);
            QuestionCreateResponseDto responseDto = questionService.createQuestion(requestDto);

            com.exit.common.grpc.QuestionCreateResponse response = com.exit.common.grpc.QuestionCreateResponse.newBuilder()
                    .setQuestionId(responseDto.questionId())
                    .setQuestionTitle(responseDto.questionTitle())
                    .setQuestionContent(responseDto.questionContent())
                    .setQuestionCategory(responseDto.questionCategory())
                    .setQuestionUrgency(responseDto.questionUrgency())
                    .setQuestionAnswerType(responseDto.questionAnswerType())
                    .setQuestionDisclosureType(responseDto.questionDisclosureType())
                    .setQuestionWriterId(responseDto.questionWriterId())
                    .setCreatedAt(toGrpcTimestamp(responseDto.createdAt()))
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
    public void answerAdopt(com.exit.common.grpc.AnswerAdoptRequest request,
                           StreamObserver<com.exit.common.grpc.AnswerAdoptResponse> responseObserver) {
        try {
            log.info("Answer adopt request received for response ID: {}", request.getResponseId());

            AnswerAdoptRequestDto requestDto = AnswerAdoptRequestDto.from(request);
            AnswerAdoptResponseDto responseDto = questionService.answerAdopt(requestDto);

            com.exit.common.grpc.AnswerAdoptResponse response = com.exit.common.grpc.AnswerAdoptResponse.newBuilder()
                    .setResponseId(responseDto.responseId())
                    .setResponseAdopt(responseDto.responseAdopt())
                    .setUpdatedAt(toGrpcTimestamp(responseDto.updatedAt()))
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
    public void answerCreate(com.exit.common.grpc.AnswerCreateRequest request,
                            StreamObserver<com.exit.common.grpc.AnswerCreateResponse> responseObserver) {
        try {
            log.info("Answer create request received for question ID: {}", request.getQuestionId());

            AnswerCreateRequestDto requestDto = AnswerCreateRequestDto.from(request);
            AnswerCreateResponseDto responseDto = questionService.answerCreate(requestDto);

            com.exit.common.grpc.AnswerCreateResponse response = com.exit.common.grpc.AnswerCreateResponse.newBuilder()
                    .setResponseId(responseDto.responseId())
                    .setQuestionId(responseDto.questionId())
                    .setResponseContent(responseDto.responseContent())
                    .setResponseWriterId(responseDto.responseWriterId())
                    .setCreatedAt(toGrpcTimestamp(responseDto.createdAt()))
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
    public void answerRecommend(com.exit.common.grpc.AnswerRecommendRequest request,
                               StreamObserver<com.exit.common.grpc.AnswerRecommendResponse> responseObserver) {
        try {
            log.info("Answer recommend request received for response ID: {}", request.getResponseId());

            AnswerRecommendRequestDto requestDto = AnswerRecommendRequestDto.from(request);
            AnswerRecommendResponseDto responseDto = questionService.toggleAnswerLike(requestDto);

            com.exit.common.grpc.AnswerRecommendResponse response = com.exit.common.grpc.AnswerRecommendResponse.newBuilder()
                    .setResponseId(requestDto.responseId())
                    .setCount(responseDto.answerLikeNum())
                    .setIsRecommended(responseDto.isLiked())
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
    public void questionReport(com.exit.common.grpc.QuestionReportRequest request,
                              StreamObserver<com.exit.common.grpc.QuestionReportResponse> responseObserver) {
        try {
            log.info("Question report request received for question ID: {}", request.getQuestionId());

            QuestionReportRequestDto requestDto = QuestionReportRequestDto.from(request);
            QuestionReportResponseDto responseDto = questionService.questionReport(requestDto);

            com.exit.common.grpc.QuestionReportResponse response = com.exit.common.grpc.QuestionReportResponse.newBuilder()
                    .setQuestionReportId(responseDto.questionReportId())
                    .setQuestionId(responseDto.questionId())
                    .setQuestionReportTitle(responseDto.questionReportTitle())
                    .setQuestionReportContent(responseDto.questionReportContent())
                    .setQuestionReportWriterId(responseDto.questionReportWriterId())
                    .setCreatedAt(toGrpcTimestamp(responseDto.createdAt()))
                    .setUpdatedAt(toGrpcTimestamp(responseDto.updatedAt()))
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
    public void answerReport(com.exit.common.grpc.AnswerReportRequest request,
                            StreamObserver<com.exit.common.grpc.AnswerReportResponse> responseObserver) {
        try {
            log.info("Answer report request received for response ID: {}", request.getResponseId());

            AnswerReportRequestDto requestDto = AnswerReportRequestDto.from(request);
            AnswerReportResponseDto responseDto = questionService.answerReport(requestDto);

            com.exit.common.grpc.AnswerReportResponse response = com.exit.common.grpc.AnswerReportResponse.newBuilder()
                    .setResponseReportId(responseDto.responseReportId())
                    .setResponseId(responseDto.responseId())
                    .setResponseReportTitle(responseDto.responseReportTitle())
                    .setResponseReportContent(responseDto.responseReportContent())
                    .setResponseReportWriterId(responseDto.responseReportWriterId())
                    .setCreatedAt(toGrpcTimestamp(responseDto.createdAt()))
                    .setUpdatedAt(toGrpcTimestamp(responseDto.updatedAt()))
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
    public void questionList(com.exit.common.grpc.QuestionListRequest request,
                            StreamObserver<com.exit.common.grpc.QuestionListResponse> responseObserver) {
        try {
            log.info("Question list request received");

            QuestionListRequestDto requestDto = QuestionListRequestDto.from(request);
            QuestionListResponseDto responseDto = questionService.questionList(requestDto);

            com.exit.common.grpc.QuestionListResponse.Builder responseBuilder = com.exit.common.grpc.QuestionListResponse.newBuilder()
                    .setHasNext(responseDto.hasNext());

            List<QuestionListItem> questionList = new ArrayList<>();
            for (QuestionListQueryResponseDto question : responseDto.questionList()) {
                com.exit.common.grpc.QuestionListItem grpcQuestion = com.exit.common.grpc.QuestionListItem.newBuilder()
                        .setQuestionId(question.questionId())
                        .setQuestionCategory(question.questionCategoryId())
                        .setQuestionWriterId(question.questionWriterId())
                        .setQuestionTitle(question.questionTitle())
                        .setQuestionContent(question.questionContent())
                        .setQuestionUrgency(question.questionUrgency())
                        .setQuestionAnswerType(question.questionAnswerType().name())
                        .setQuestionAnswerAdopt(question.questionAnswerAdopt())
                        .setAnswerCount(question.answerCount())
                        .setCreatedAt(toGrpcTimestamp(question.createdAt()))
                        .build();
                questionList.add(grpcQuestion);
            }
            responseBuilder.addAllQuestions(questionList);

            responseObserver.onNext(responseBuilder.build());
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

            CategoryRecommendationResponseDto responseDto = questionService.categoryRecommend(request.getTitle());

            if (responseDto == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("추천할 카테고리를 찾을 수 없습니다")
                        .asRuntimeException());
                return;
            }

            com.exit.common.grpc.CategoryRecommendationResponse response = com.exit.common.grpc.CategoryRecommendationResponse.newBuilder()
                    .setCategoryId(responseDto.questionCategoryId())
                    .setCategoryName(responseDto.questionCategoryName())
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
    public void similarQuestion(com.exit.common.grpc.SimilarQuestionRequest request,
                               StreamObserver<com.exit.common.grpc.SimilarQuestionResponse> responseObserver) {
        try {
            log.info("Similar question request received for title: {}", request.getTitle());

            SimilarQuestionResponseDto responseDto = questionService.similarQuestion(request.getTitle());

            if (responseDto == null) {
                responseObserver.onError(Status.NOT_FOUND
                        .withDescription("유사한 질문을 찾을 수 없습니다")
                        .asRuntimeException());
                return;
            }

            com.exit.common.grpc.SimilarQuestionResponse response = com.exit.common.grpc.SimilarQuestionResponse.newBuilder()
                    .setQuestionId(responseDto.questionId())
                    .setQuestionTitle(responseDto.questionTitle())
                    .setQuestionContent(responseDto.questionContent())
                    .setQuestionCategory(responseDto.questionCategory().getQuestionCategoryId())
                    .setQuestionUrgency(responseDto.questionUrgency())
                    .setQuestionAnswerType(responseDto.questionAnswerType().name())
                    .setQuestionAnswerAdopt(responseDto.questionAnswerAdopt())
                    .setCreatedAt(toGrpcTimestamp(responseDto.createdAt()))
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

    @Override
    public void getQuestionDetail(com.exit.common.grpc.QuestionDetailRequest request,
                                 StreamObserver<com.exit.common.grpc.QuestionDetailResponse> responseObserver) {
        try {
            log.info("Question detail request received for question ID: {}", request.getQuestionId());

            QuestionDetailResponseDto responseDto = questionService.getQuestionDetail(request.getQuestionId());

            // Convert QuestionCreateResponseDto to gRPC QuestionCreateResponse
            com.exit.common.grpc.QuestionCreateResponse.Builder questionBuilder = com.exit.common.grpc.QuestionCreateResponse.newBuilder()
                    .setQuestionId(responseDto.question().questionId())
                    .setQuestionTitle(responseDto.question().questionTitle())
                    .setQuestionContent(responseDto.question().questionContent())
                    .setQuestionCategory(responseDto.question().questionCategory())
                    .setQuestionUrgency(responseDto.question().questionUrgency())
                    .setQuestionAnswerType(responseDto.question().questionAnswerType())
                    .setQuestionDisclosureType(responseDto.question().questionDisclosureType())
                    .setQuestionWriterId(responseDto.question().questionWriterId())
                    .setQuestionWriterName(responseDto.question().questionWriterName())
                    .setCreatedAt(toGrpcTimestamp(responseDto.question().createdAt()));

            // Add question image URLs if they exist
            if (responseDto.question().imageUrls() != null) {
                questionBuilder.addAllImageUrls(responseDto.question().imageUrls());
            }

            // Convert ResponseDetailDto list to gRPC ResponseDetail list
            List<com.exit.common.grpc.ResponseDetail> grpcResponses = new ArrayList<>();
            for (ResponseDetailDto response : responseDto.responses()) {
                com.exit.common.grpc.ResponseDetail.Builder responseDetailBuilder = com.exit.common.grpc.ResponseDetail.newBuilder()
                        .setResponseId(response.responseId())
                        .setResponseWriterId(response.responseWriterId())
                        .setResponseWriterName(response.responseWriterName() != null ? response.responseWriterName() : "")
                        .setResponseContent(response.responseContent())
                        .setResponseAdopt(response.responseAdopt())
                        .setLikeCount(response.likeCount())
                        .setCreatedAt(response.createdAt().toString())
                        .setUpdatedAt(response.updatedAt().toString());

                // Add response image URLs if they exist
                if (response.urls() != null) {
                    responseDetailBuilder.addAllUrls(response.urls());
                }

                grpcResponses.add(responseDetailBuilder.build());
            }

            com.exit.common.grpc.QuestionDetailResponse grpcResponse = com.exit.common.grpc.QuestionDetailResponse.newBuilder()
                    .setQuestion(questionBuilder.build())
                    .addAllResponses(grpcResponses)
                    .setHasNext(responseDto.hasNext())
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Question detail failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("질문 상세 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }
}