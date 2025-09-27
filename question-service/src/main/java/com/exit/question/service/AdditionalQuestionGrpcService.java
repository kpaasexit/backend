package com.exit.question.service;

import com.exit.common.grpc.AdditionalQuestionServiceGrpc;
import com.exit.common.grpc.CreateAdditionalQuestionMessageResponse;
import com.exit.common.grpc.GetAdditionalQuestionResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AdditionalQuestionGrpcService extends AdditionalQuestionServiceGrpc.AdditionalQuestionServiceImplBase {

    private final AdditionalQuestionService additionalQuestionService;

    @Override
    public void createAdditionalQuestionMessage(com.exit.common.grpc.CreateAdditionalQuestionMessageRequest request,
                                                StreamObserver<CreateAdditionalQuestionMessageResponse> responseObserver) {
        try {
            log.info("Additional Question create request received: {}", request.getQuestionId());
            CreateAdditionalQuestionMessageResponse response = additionalQuestionService.createAdditionalQuestionMessage(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Additional Question create failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("추가 질문 생성 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }


    @Override
    public void getAdditionalQuestion(com.exit.common.grpc.GetAdditionalQuestionRequest request,
                                      StreamObserver<com.exit.common.grpc.GetAdditionalQuestionResponse> responseObserver) {
        try {
            log.info("Get Additional Question Message List : {}", request.getFollowUpRoomId());
            GetAdditionalQuestionResponse response = additionalQuestionService.getAdditionalQuestion(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Get Additional Question failed", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("추가 질문 메세지 조회 중 오류가 발생했습니다")
                    .asRuntimeException());
        }
    }
}
