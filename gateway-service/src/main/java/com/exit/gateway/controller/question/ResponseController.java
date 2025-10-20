package com.exit.gateway.controller.question;

import com.exit.common.grpc.*;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.QuestionSuccessCode;
import com.exit.gateway.controller.question.dto.request.question.AnswerCreateRequestDto;
import com.exit.gateway.controller.question.dto.request.question.AnswerReportRequestDto;
import com.exit.gateway.controller.question.dto.request.question.AnswerUpdateRequestDto;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.controller.question.dto.response.response.GetAiBestResponseDto;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.question.QuestionRequestMapper;
import com.exit.gateway.service.question.ResponseGrpcClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/responses")
@RequiredArgsConstructor
@Slf4j
public class ResponseController {
    private final ResponseGrpcClient responseGrpcClient;
    private final QuestionRequestMapper questionRequestMapper;

    @PostMapping(value = "/answers", consumes = "multipart/form-data")
    public SuccessResponse<AnswerCreateResponseDto> createAnswer(
            @Valid @ModelAttribute AnswerCreateRequestDto request,
            @LoginUser Long userId
    ) {
        log.info("Answer create request received");
        AnswerCreateRequest grpcRequest = questionRequestMapper.toGrpcAnswerCreateRequest(request, userId);
        AnswerCreateResponseDto response = responseGrpcClient.createAnswer(grpcRequest);
        return SuccessResponse.of(QuestionSuccessCode.ANSWER_CREATE_SUCCESS, response);
    }

    @PostMapping("/answers/{responseId}/adopt")
    public SuccessResponse<AnswerAdoptResponseDto> adoptAnswer(@PathVariable Long responseId) {
        log.info("Answer adopt request received for responseId: {}", responseId);
        AnswerAdoptRequest request = AnswerAdoptRequest.newBuilder()
                .setResponseId(responseId)
                .build();
        AnswerAdoptResponseDto response = responseGrpcClient.adoptAnswer(request);
        return SuccessResponse.of(QuestionSuccessCode.ANSWER_ADOPT_SUCCESS, response);
    }

    @PostMapping("/answers/{responseId}/recommend")
    public SuccessResponse<AnswerRecommendResponseDto> recommendAnswer(
            @PathVariable Long responseId,
            @LoginUser Long userId
    ) {
        log.info("Answer recommend request received for responseId: {}, userId: {}", responseId, userId);
        AnswerRecommendRequest request = AnswerRecommendRequest.newBuilder()
                .setResponseId(responseId)
                .setUserId(userId)
                .build();
        AnswerRecommendResponseDto response = responseGrpcClient.recommendAnswer(request);
        return SuccessResponse.of(QuestionSuccessCode.ANSWER_RECOMMEND_SUCCESS, response);
    }

    @PostMapping("/answers/{responseId}/report")
    public SuccessResponse<AnswerReportResponseDto> reportAnswer(
            @PathVariable Long responseId,
            @Valid @RequestBody AnswerReportRequestDto request
    ) {
        log.info("Answer report request received for responseId: {}", responseId);
        AnswerReportRequest grpcRequest = questionRequestMapper.toGrpcAnswerReportRequest(responseId, request);
        AnswerReportResponseDto response = responseGrpcClient.reportAnswer(grpcRequest);
        return SuccessResponse.of(QuestionSuccessCode.ANSWER_REPORT_SUCCESS, response);
    }

    @PutMapping("/answers/{responseId}")
    public SuccessResponse<AnswerUpdateResponseDto> updateAnswer(
            @PathVariable Long responseId,
            @Valid @RequestBody AnswerUpdateRequestDto request
    ) {
        log.info("Answer update request received for responseId: {}", responseId);
        UpdateResponseRequest grpcRequest = UpdateResponseRequest.newBuilder()
                .setResponseId(responseId)
                .setContent(request.content())
                .build();

        return SuccessResponse.of(QuestionSuccessCode.ANSWER_UPDATE_SUCCESS,
                responseGrpcClient.updateResponse(grpcRequest));
    }

    @DeleteMapping("/answers/{responseId}")
    public SuccessResponse<String> deleteAnswer(@PathVariable Long responseId) {
        log.info("Answer delete request received for responseId: {}", responseId);
        DeleteResponseRequest grpcRequest = DeleteResponseRequest.newBuilder()
                .setResponseId(responseId)
                .build();
        responseGrpcClient.deleteResponse(grpcRequest);
        return SuccessResponse.of(QuestionSuccessCode.ANSWER_DELETE_SUCCESS, "성공적으로 삭제하였습니다.");
    }

    @GetMapping("/{questionId}/responses")
    public SuccessResponse<GetDetailResponseResponseDto> getDetailResponse(
            @PathVariable Long questionId,
            @LoginUser Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer size
    ) {
        log.info("Get detail response for userId: {}, pageNum: {}", userId, pageNum);
        return SuccessResponse.of(QuestionSuccessCode.GET_DETAIL_RESPONSE,
                responseGrpcClient.getDetailResponse(questionId, userId, pageNum - 1, size));
    }

    @GetMapping("/ai")
    public SuccessResponse<GetAiBestResponseDto> getBestAiResponse() {
        log.info("Get best ai response");
        return SuccessResponse.of(QuestionSuccessCode.GET_DETAIL_RESPONSE,
                responseGrpcClient.getBestAiResponse());
    }
}
