package com.exit.question.service.grpc;

import com.exit.common.grpc.AdditionalQuestionServiceGrpc;
import com.exit.common.grpc.CreateAdditionalQuestionMessageResponse;
import com.exit.common.grpc.GetAdditionalQuestionResponse;
import com.exit.question.service.AdditionalQuestionService;
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
            log.info("Additional Question create request received: {}", request.getQuestionId());
            CreateAdditionalQuestionMessageResponse response = additionalQuestionService.createAdditionalQuestionMessage(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }


    @Override
    public void getAdditionalQuestion(com.exit.common.grpc.GetAdditionalQuestionRequest request,
                                      StreamObserver<com.exit.common.grpc.GetAdditionalQuestionResponse> responseObserver) {
            log.info("Get Additional Question Message List : {}", request.getFollowUpRoomId());
            GetAdditionalQuestionResponse response = additionalQuestionService.getAdditionalQuestion(request);

            responseObserver.onNext(response);
            responseObserver.onCompleted();
    }
}
