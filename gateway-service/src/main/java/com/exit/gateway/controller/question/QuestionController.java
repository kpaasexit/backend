package com.exit.gateway.controller.question;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.*;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.QuestionErrorCode;
import com.exit.common.response.success.QuestionSuccessCode;
import com.exit.gateway.controller.question.dto.request.question.AnswerCreateRequestDto;
import com.exit.gateway.controller.question.dto.request.question.AnswerReportRequestDto;
import com.exit.gateway.controller.question.dto.request.question.AnswerUpdateRequestDto;
import com.exit.gateway.controller.question.dto.request.question.QuestionCreateRequestDto;
import com.exit.gateway.controller.question.dto.request.question.QuestionReportRequestDto;
import com.exit.gateway.controller.question.dto.response.question.*;
import com.exit.gateway.service.question.QuestionGrpcClient;
import com.exit.gateway.service.question.QuestionRequestMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {

    private final QuestionGrpcClient questionGrpcClient;
    private final QuestionRequestMapper questionRequestMapper;

    @PostMapping(consumes = "multipart/form-data")
    public SuccessResponse<QuestionCreateResponseDto> createQuestion(
            @Valid @ModelAttribute QuestionCreateRequestDto request) {
        try {
            log.info("Question create request received");
            QuestionCreateRequest grpcRequest = questionRequestMapper.toGrpcQuestionCreateRequest(request);
            QuestionCreateResponseDto response = questionGrpcClient.createQuestion(grpcRequest);
            return SuccessResponse.of(QuestionSuccessCode.QUESTION_CREATE_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Question create failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Question create failed", e);
            throw new RuntimeException("질문 등록에 실패했습니다.");
        }
    }

    @GetMapping
    public SuccessResponse<QuestionListResponseDto> getQuestionList(
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Question list request received");

            QuestionListRequest.Builder requestBuilder = QuestionListRequest.newBuilder()
                    .setKeyword(keyword != null ? keyword : "")
                    .setPage(page)
                    .setSize(size);

            if (categoryIds != null) {
                requestBuilder.addAllCategoryIds(categoryIds);
            }

            QuestionListResponseDto response = questionGrpcClient.getQuestionList(requestBuilder.build());
            return SuccessResponse.of(QuestionSuccessCode.QUESTION_LIST_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Question list failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Question list failed", e);
            throw new RuntimeException("질문 목록 조회에 실패했습니다.");
        }
    }

    @GetMapping("/{questionId}")
    public SuccessResponse<QuestionDetailResponseDto> getQuestionDetail(@PathVariable Long questionId) {
        try {
            log.info("Question detail request received for questionId: {}", questionId);
            QuestionDetailRequest request = QuestionDetailRequest.newBuilder()
                    .setQuestionId(questionId)
                    .build();
            QuestionDetailResponseDto response = questionGrpcClient.getQuestionDetail(request);
            return SuccessResponse.of(QuestionSuccessCode.QUESTION_DETAIL_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Question detail failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Question detail failed", e);
            throw new RuntimeException("질문 상세 조회에 실패했습니다.");
        }
    }

    @PostMapping(value = "/answers", consumes = "multipart/form-data")
    public SuccessResponse<AnswerCreateResponseDto> createAnswer(
            @Valid @ModelAttribute AnswerCreateRequestDto request) {
        try {
            log.info("Answer create request received");
            AnswerCreateRequest grpcRequest = questionRequestMapper.toGrpcAnswerCreateRequest(request);
            AnswerCreateResponseDto response = questionGrpcClient.createAnswer(grpcRequest);
            return SuccessResponse.of(QuestionSuccessCode.ANSWER_CREATE_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Answer create failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Answer create failed", e);
            throw new RuntimeException("답변 등록에 실패했습니다.");
        }
    }

    @PostMapping("/answers/{responseId}/adopt")
    public SuccessResponse<AnswerAdoptResponseDto> adoptAnswer(@PathVariable Long responseId) {
        try {
            log.info("Answer adopt request received for responseId: {}", responseId);
            AnswerAdoptRequest request = AnswerAdoptRequest.newBuilder()
                    .setResponseId(responseId)
                    .build();
            AnswerAdoptResponseDto response = questionGrpcClient.adoptAnswer(request);
            return SuccessResponse.of(QuestionSuccessCode.ANSWER_ADOPT_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Answer adopt failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Answer adopt failed", e);
            throw new RuntimeException("답변 채택에 실패했습니다.");
        }
    }

    @PostMapping("/answers/{responseId}/recommend")
    public SuccessResponse<AnswerRecommendResponseDto> recommendAnswer(
            @PathVariable Long responseId,
            @AuthenticationPrincipal Long userId) {
        try {
            log.info("Answer recommend request received for responseId: {}, userId: {}", responseId, userId);
            AnswerRecommendRequest request = AnswerRecommendRequest.newBuilder()
                    .setResponseId(responseId)
                    .setUserId(userId)
                    .build();
            AnswerRecommendResponseDto response = questionGrpcClient.recommendAnswer(request);
            return SuccessResponse.of(QuestionSuccessCode.ANSWER_RECOMMEND_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Answer recommend failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Answer recommend failed", e);
            throw new RuntimeException("답변 추천에 실패했습니다.");
        }
    }

    @PostMapping("/{questionId}/report")
    public SuccessResponse<QuestionReportResponseDto> reportQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QuestionReportRequestDto request) {
        try {
            log.info("Question report request received for questionId: {}", questionId);
            QuestionReportRequest grpcRequest = questionRequestMapper.toGrpcQuestionReportRequest(questionId, userId, request);
            QuestionReportResponseDto response = questionGrpcClient.reportQuestion(grpcRequest);
            return SuccessResponse.of(QuestionSuccessCode.QUESTION_REPORT_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Question report failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Question report failed", e);
            throw new RuntimeException("질문 신고에 실패했습니다.");
        }
    }

    @PostMapping("/answers/{responseId}/report")
    public SuccessResponse<AnswerReportResponseDto> reportAnswer(
            @PathVariable Long responseId,
            @Valid @RequestBody AnswerReportRequestDto request) {
        try {
            log.info("Answer report request received for responseId: {}", responseId);
            AnswerReportRequest grpcRequest = questionRequestMapper.toGrpcAnswerReportRequest(responseId, request);
            AnswerReportResponseDto response = questionGrpcClient.reportAnswer(grpcRequest);
            return SuccessResponse.of(QuestionSuccessCode.ANSWER_REPORT_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Answer report failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Answer report failed", e);
            throw new RuntimeException("답변 신고에 실패했습니다.");
        }
    }

    @GetMapping("/categories/recommend")
    public SuccessResponse<CategoryRecommendationResponseDto> recommendCategory(
            @RequestParam String title) {
        try {
            log.info("Category recommend request received for title: {}", title);
            CategoryRecommendRequest request = CategoryRecommendRequest.newBuilder()
                    .setTitle(title)
                    .build();
            CategoryRecommendationResponseDto response = questionGrpcClient.recommendCategory(request);
            return SuccessResponse.of(QuestionSuccessCode.CATEGORY_RECOMMEND_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Category recommend failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Category recommend failed", e);
            throw new RuntimeException("카테고리 추천에 실패했습니다.");
        }
    }

    @GetMapping("/similar")
    public SuccessResponse<SimilarQuestionResponseDto> getSimilarQuestion(
            @RequestParam String title) {
        try {
            log.info("Similar question request received for title: {}", title);
            SimilarQuestionRequest request = SimilarQuestionRequest.newBuilder()
                    .setTitle(title)
                    .build();
            SimilarQuestionResponseDto response = questionGrpcClient.getSimilarQuestion(request);
            return SuccessResponse.of(QuestionSuccessCode.SIMILAR_QUESTION_SUCCESS, response);
        } catch (StatusRuntimeException e) {
            log.error("Similar question failed via gRPC: {}", e.getStatus(), e);
            throw new RuntimeException(getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Similar question failed", e);
            throw new RuntimeException("유사 질문 조회에 실패했습니다.");
        }
    }

    @PutMapping("/answers/{responseId}")
    public SuccessResponse<AnswerUpdateResponseDto> updateAnswer(
            @PathVariable Long responseId,
            @Valid @RequestBody AnswerUpdateRequestDto request) {
        try {
            log.info("Answer update request received for responseId: {}", responseId);
            UpdateResponseRequest grpcRequest = UpdateResponseRequest.newBuilder()
                    .setResponseId(responseId)
                    .setContent(request.content())
                    .build();

            return SuccessResponse.of(QuestionSuccessCode.ANSWER_UPDATE_SUCCESS,
                    questionGrpcClient.updateResponse(grpcRequest));
        } catch (StatusRuntimeException e) {
            log.error("Answer update failed via gRPC: {}", e.getStatus(), e);
            throw new RestApiException(QuestionErrorCode.UPDATE_RESPONSE_FAIL, getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Answer update failed", e);
            throw new RestApiException(QuestionErrorCode.UPDATE_RESPONSE_FAIL);
        }
    }

    @DeleteMapping("/answers/{responseId}")
    public SuccessResponse<String> deleteAnswer(@PathVariable Long responseId) {
        try {
            log.info("Answer delete request received for responseId: {}", responseId);
            DeleteResponseRequest grpcRequest = DeleteResponseRequest.newBuilder()
                    .setResponseId(responseId)
                    .build();
            questionGrpcClient.deleteResponse(grpcRequest);
            return SuccessResponse.of(QuestionSuccessCode.ANSWER_DELETE_SUCCESS, "성공적으로 삭제하였습니다.");
        } catch (StatusRuntimeException e) {
            log.error("Answer delete failed via gRPC: {}", e.getStatus(), e);
            throw new RestApiException(QuestionErrorCode.DELETE_RESPONSE_FAIL, getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Answer delete failed", e);
            throw new RestApiException(QuestionErrorCode.DELETE_RESPONSE_FAIL);
        }
    }

    private String getGrpcErrorMessage(StatusRuntimeException e) {
        Status status = e.getStatus();
        switch (status.getCode()) {
            case INVALID_ARGUMENT:
                return "잘못된 요청입니다.";
            case UNAUTHENTICATED:
                return "인증에 실패했습니다.";
            case PERMISSION_DENIED:
                return "권한이 없습니다.";
            case NOT_FOUND:
                return "요청한 데이터를 찾을 수 없습니다.";
            case ALREADY_EXISTS:
                return "이미 존재하는 데이터입니다.";
            default:
                return status.getDescription() != null ? status.getDescription() : "서버 오류가 발생했습니다.";
        }
    }
}