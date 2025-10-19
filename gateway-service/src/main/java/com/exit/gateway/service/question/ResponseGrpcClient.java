package com.exit.gateway.service.question;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.question.ResponseGrpcErrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@GrpcToRest(mapper = ResponseGrpcErrorMapper.class)
public class ResponseGrpcClient {
    @GrpcClient("question-service")
    private ResponseServiceGrpc.ResponseServiceBlockingStub responseServiceStub;

    public AnswerCreateResponseDto createAnswer(AnswerCreateRequest request) {
        log.debug("Sending answer create request via gRPC");
        AnswerCreateResponse response = responseServiceStub.answerCreate(request);
        log.debug("Received answer create response via gRPC");
        return AnswerCreateResponseDto.from(response);
    }

    public AnswerRecommendResponseDto recommendAnswer(AnswerRecommendRequest request) {
        log.debug("Sending answer recommend request via gRPC");
        AnswerRecommendResponse response = responseServiceStub.answerRecommend(request);
        log.debug("Received answer recommend response via gRPC");
        return AnswerRecommendResponseDto.from(response);
    }

    public AnswerReportResponseDto reportAnswer(AnswerReportRequest request) {
        log.debug("Sending answer report request via gRPC");
        AnswerReportResponse response = responseServiceStub.answerReport(request);
        log.debug("Received answer report response via gRPC");
        return AnswerReportResponseDto.from(response);
    }

    public AnswerAdoptResponseDto adoptAnswer(AnswerAdoptRequest request) {
        log.debug("Sending answer adopt request via gRPC");
        AnswerAdoptResponse response = responseServiceStub.answerAdopt(request);
        log.debug("Received answer adopt response via gRPC");
        return AnswerAdoptResponseDto.from(response);
    }

    public AnswerUpdateResponseDto updateResponse(UpdateResponseRequest request) {
        log.debug("Sending update response request via gRPC for responseId: {}", request.getResponseId());
        UpdateResponseResponse response = responseServiceStub.updateResponse(request);
        log.debug("Received update response response via gRPC");
        return new AnswerUpdateResponseDto(response.getResponseId(), response.getContent());
    }

    public void deleteResponse(DeleteResponseRequest request) {
        log.debug("Sending delete response request via gRPC for responseId: {}", request.getResponseId());
        responseServiceStub.deleteResponse(request);
        log.debug("Received delete response response via gRPC");
    }

    public GetDetailResponseResponseDto getDetailResponse(Long questionId, Long userId, Integer pageNum) {
        log.debug("Sending get detail response request via gRPC");
        GetDetailResponseRequest request = GetDetailResponseRequest.newBuilder()
                .setQuestionId(questionId)
                .setUserId(userId)
                .setPageNum(pageNum)
                .build();
        GetDetailResponseResponse response = responseServiceStub.getDetailResponse(request);
        log.debug("Received get detail response response via gRPC");
        return GetDetailResponseResponseDto.from(response);
    }
}
