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
            log.info("Answer report request received for response ID: {}", request.getResponseId());

            AnswerReportResponse response = responseService.answerReport(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void answerAdopt(com.exit.common.grpc.AnswerAdoptRequest request,
                            StreamObserver<com.exit.common.grpc.AnswerAdoptResponse> responseObserver) {
            log.info("Answer adopt request received for response ID: {}", request.getResponseId());
            AnswerAdoptResponse response = responseService.answerAdopt(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void getDetailResponse(com.exit.common.grpc.GetDetailResponseRequest request,
                                  StreamObserver<com.exit.common.grpc.GetDetailResponseResponse> responseObserver) {
            log.info("Get Detail Response request received for question ID: {}", request.getQuestionId());
            GetDetailResponseResponse response = responseService.getDetailResponse(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void answerCreate(com.exit.common.grpc.AnswerCreateRequest request,
                             StreamObserver<com.exit.common.grpc.AnswerCreateResponse> responseObserver) {
            log.info("Answer create request received for question ID: {}", request.getQuestionId());

            AnswerCreateResponse response = responseService.answerCreate(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void answerRecommend(com.exit.common.grpc.AnswerRecommendRequest request,
                                StreamObserver<com.exit.common.grpc.AnswerRecommendResponse> responseObserver) {
            log.info("Answer recommend request received for response ID: {}", request.getResponseId());
            AnswerRecommendResponse response = responseService.toggleAnswerLike(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void updateResponse(com.exit.common.grpc.UpdateResponseRequest request,
                               StreamObserver<com.exit.common.grpc.UpdateResponseResponse> responseObserver) {
            log.info("Update response request received for response id: {}", request.getResponseId());

            UpdateResponseResponse response = responseService.updateResponse(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }

    @Override
    public void deleteResponse(com.exit.common.grpc.DeleteResponseRequest request,
                               StreamObserver<com.google.protobuf.Empty> responseObserver) {
            log.info("Delete response request received for response ID: {}", request.getResponseId());

            responseService.deleteResponse(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
    }

    @Override
    public void getAiBestResponse(com.google.protobuf.Empty request,
                               StreamObserver<GetAiBestResponseResponse> responseObserver) {
            log.info("Get Ai best response request received");
            GetAiBestResponseResponse response = responseService.getAiBestResponse();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }
}