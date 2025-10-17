package com.exit.gateway.service.question;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.question.dto.response.question.CreateAdditionalQuestionMessageResponseDto;
import com.exit.gateway.controller.question.dto.response.question.GetAdditionalQuestionResponseDto;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.question.AdditionalQuestionGrpcErrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@GrpcToRest(mapper = AdditionalQuestionGrpcErrorMapper.class)
public class AdditionalQuestionGrpcClient {

    @GrpcClient("question-service")
    private AdditionalQuestionServiceGrpc.AdditionalQuestionServiceBlockingStub additionalQuestionServiceBlockingStub;

    public CreateAdditionalQuestionMessageResponseDto createAdditionalQuestionMessage(CreateAdditionalQuestionMessageRequest request) {
        log.debug("Sending create additional question message request via gRPC");
        CreateAdditionalQuestionMessageResponse response = additionalQuestionServiceBlockingStub.createAdditionalQuestionMessage(request);
        log.debug("Received create additional question message response via gRPC");
        return CreateAdditionalQuestionMessageResponseDto.from(response);
    }

    public GetAdditionalQuestionResponseDto getAdditionalQuestion(GetAdditionalQuestionRequest request) {
        log.debug("Sending get additional question request via gRPC for followUpRoomId: {}", request.getFollowUpRoomId());
        GetAdditionalQuestionResponse response = additionalQuestionServiceBlockingStub.getAdditionalQuestion(request);
        log.debug("Received get additional question response via gRPC");
        return GetAdditionalQuestionResponseDto.from(response);
    }
}