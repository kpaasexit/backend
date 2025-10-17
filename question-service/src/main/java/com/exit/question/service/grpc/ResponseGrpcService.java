package com.exit.question.service.grpc;

import com.exit.common.grpc.*;
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
public class ResponseGrpcService extends ResponseServiceGrpc.ResponseServiceImplBase {
    private final ResponseService responseService;

    @Override
    public void answerReport(com.exit.common.grpc.AnswerReportRequest request,
                             StreamObserver<AnswerReportResponse> responseObserver) {
        try {
            log.info("Answer report request received for response ID: {}", request.getResponseId());

            AnswerReportResponse response = responseService.answerReport(request);

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
    public void answerAdopt(com.exit.common.grpc.AnswerAdoptRequest request,
                            StreamObserver<com.exit.common.grpc.AnswerAdoptResponse> responseObserver) {
        try {
            log.info("Answer adopt request received for response ID: {}", request.getResponseId());
            AnswerAdoptResponse response = responseService.answerAdopt(request);

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
    public void getDetailResponse(com.exit.common.grpc.GetDetailResponseRequest request,
                                  StreamObserver<com.exit.common.grpc.GetDetailResponseResponse> responseObserver) {
        try {
            log.info("Get Detail Response request received for question ID: {}", request.getQuestionId());
            GetDetailResponseResponse response = responseService.getDetailResponse(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Get Detail Response failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("답변 상세 조회 중 오류가 발생했습니다")
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