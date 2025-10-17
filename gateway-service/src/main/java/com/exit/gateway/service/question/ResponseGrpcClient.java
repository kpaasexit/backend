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
public class ResponseGrpcClient {
    @GrpcClient("question-service")
    private ResponseServiceGrpc.ResponseServiceBlockingStub responseServiceStub;

    public AnswerCreateResponseDto createAnswer(AnswerCreateRequest request) {
        try {
            log.debug("Sending answer create request via gRPC");
            AnswerCreateResponse response = responseServiceStub.answerCreate(request);
            log.debug("Received answer create response via gRPC");
            return AnswerCreateResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer create failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerRecommendResponseDto recommendAnswer(AnswerRecommendRequest request) {
        try {
            log.debug("Sending answer recommend request via gRPC");
            AnswerRecommendResponse response = responseServiceStub.answerRecommend(request);
            log.debug("Received answer recommend response via gRPC");
            return AnswerRecommendResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer recommend failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerReportResponseDto reportAnswer(AnswerReportRequest request) {
        try {
            log.debug("Sending answer report request via gRPC");
            AnswerReportResponse response = responseServiceStub.answerReport(request);
            log.debug("Received answer report response via gRPC");
            return AnswerReportResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer report failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerAdoptResponseDto adoptAnswer(AnswerAdoptRequest request) {
        try {
            log.debug("Sending answer adopt request via gRPC");
            AnswerAdoptResponse response = responseServiceStub.answerAdopt(request);
            log.debug("Received answer adopt response via gRPC");
            return AnswerAdoptResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC answer adopt failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public AnswerUpdateResponseDto updateResponse(UpdateResponseRequest request) {
        try {
            log.debug("Sending update response request via gRPC for responseId: {}", request.getResponseId());
            UpdateResponseResponse response = responseServiceStub.updateResponse(request);
            log.debug("Received update response response via gRPC");
            return new AnswerUpdateResponseDto(response.getResponseId(), response.getContent());
        } catch (StatusRuntimeException e) {
            log.error("gRPC update response failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public void deleteResponse(DeleteResponseRequest request) {
        try {
            log.debug("Sending delete response request via gRPC for responseId: {}", request.getResponseId());
            responseServiceStub.deleteResponse(request);
            log.debug("Received delete response response via gRPC");
        } catch (StatusRuntimeException e) {
            log.error("gRPC delete response failed: {}", e.getStatus(), e);
            throw e;
        }
    }

    public GetDetailResponseResponseDto getDetailResponse(Long questionId, Long userId, Integer pageNum) {
        try {
            log.debug("Sending get detail response request via gRPC");
            GetDetailResponseRequest request = GetDetailResponseRequest.newBuilder()
                    .setQuestionId(questionId)
                    .setUserId(userId)
                    .setPageNum(pageNum)
                    .build();
            GetDetailResponseResponse response = responseServiceStub.getDetailResponse(request);
            log.debug("Received get detail response response via gRPC");
            return GetDetailResponseResponseDto.from(response);
        } catch (StatusRuntimeException e) {
            log.error("gRPC get detail response failed: {}", e.getStatus(), e);
            throw e;
        }
    }
}
