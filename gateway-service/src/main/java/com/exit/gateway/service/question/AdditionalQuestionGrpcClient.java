package com.exit.gateway.service.question;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.question.dto.response.question.*;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdditionalQuestionGrpcClient {

    @GrpcClient("question-service")
    private AdditionalQuestionServiceGrpc.AdditionalQuestionServiceBlockingStub additionalQuestionServiceBlockingStub;

    public CreateAdditionalQuestionMessageResponseDto createAdditionalQuestionMessage(CreateAdditionalQuestionMessageRequest request) {
        try {
            log.debug("Sending create additional question message request via gRPC");
            CreateAdditionalQuestionMessageResponse response = additionalQuestionServiceBlockingStub.createAdditionalQuestionMessage(request);
            log.debug("Received create additional question message response via gRPC");
            return CreateAdditionalQuestionMessageResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC create additional question message failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public GetAdditionalQuestionResponseDto getAdditionalQuestion(GetAdditionalQuestionRequest request) {
        try {
            log.debug("Sending get additional question request via gRPC for followUpRoomId: {}", request.getFollowUpRoomId());
            GetAdditionalQuestionResponse response = additionalQuestionServiceBlockingStub.getAdditionalQuestion(request);
            log.debug("Received get additional question response via gRPC");
            return GetAdditionalQuestionResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC get additional question failed: {}", e.getStatus(), e);
            throw e;
        }
    }
}