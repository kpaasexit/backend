package com.exit.gateway.controller.question;

import com.exit.common.grpc.CreateAdditionalQuestionMessageRequest;
import com.exit.common.grpc.GetAdditionalQuestionRequest;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.QuestionSuccessCode;
import com.exit.gateway.controller.question.dto.request.question.CreateAdditionalQuestionMessageRequestDto;
import com.exit.gateway.controller.question.dto.response.question.CreateAdditionalQuestionMessageResponseDto;
import com.exit.gateway.controller.question.dto.response.question.GetAdditionalQuestionResponseDto;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.question.AdditionalQuestionGrpcClient;
import com.exit.gateway.service.question.QuestionRequestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/additional-question")
@RequiredArgsConstructor
@Slf4j
public class AdditionalQuestionController {
    private final AdditionalQuestionGrpcClient additionalQuestionGrpcClient;
    private final QuestionRequestMapper questionRequestMapper;

    @PostMapping(value = "/message", consumes = "multipart/form-data")
    public SuccessResponse<CreateAdditionalQuestionMessageResponseDto> createAdditionalQuestionMessage(
            @LoginUser Long userId,
            @Valid @ModelAttribute CreateAdditionalQuestionMessageRequestDto request
    ) {
        log.info("Create additional question message request received");
        CreateAdditionalQuestionMessageRequest grpcRequest = questionRequestMapper.toGrpcCreateAdditionalQuestionMessageRequest(userId, request);

        return SuccessResponse.of(QuestionSuccessCode.ADDITIONAL_QUESTION_CREATE_SUCCESS,
                additionalQuestionGrpcClient.createAdditionalQuestionMessage(grpcRequest));
    }

    @GetMapping("/{followUpRoomId}")
    public SuccessResponse<GetAdditionalQuestionResponseDto> getAdditionalQuestion(
            @PathVariable Long followUpRoomId,
            @RequestParam Long questionId,
            @LoginUser Long userId
    ) {
        log.info("Get additional question request received for followUpRoomId: {}, questionId: {}", followUpRoomId, questionId);

        GetAdditionalQuestionRequest grpcRequest = GetAdditionalQuestionRequest.newBuilder()
                .setFollowUpRoomId(followUpRoomId)
                .setQuestionId(questionId)
                .setUserId(userId)
                .build();

        return SuccessResponse.of(QuestionSuccessCode.QUESTION_DETAIL_SUCCESS,
                additionalQuestionGrpcClient.getAdditionalQuestion(grpcRequest));
    }
}
