package com.exit.gateway.controller.question;

import com.exit.common.exception.rest.RestApiException;
import com.exit.common.grpc.CreateCommentRequest;
import com.exit.common.grpc.DeleteCommentRequest;
import com.exit.common.grpc.GetCommentRequest;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.error.rest.QuestionErrorCode;
import com.exit.common.response.success.QuestionSuccessCode;
import com.exit.gateway.controller.question.dto.request.comment.CreateCommentRequestDto;
import com.exit.gateway.controller.question.dto.response.comment.GetCommentResponseDto;
import com.exit.gateway.controller.question.dto.response.question.CreateCommentResponseDto;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.question.CommentGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentGrpcClient commentGrpcClient;

    @PostMapping
    public SuccessResponse<CreateCommentResponseDto> createComment(
            @Valid @RequestBody CreateCommentRequestDto request,
            @LoginUser Long userId) {
        try {
            log.info("Comment create request received for targetId: {}, commentType: {}",
                    request.targetId(), request.commentType());

            CreateCommentRequest grpcRequest = CreateCommentRequest.newBuilder()
                    .setTargetId(request.targetId())
                    .setCommentType(request.commentType())
                    .setWriterId(userId)
                    .setContent(request.content())
                    .build();

            return SuccessResponse.of(QuestionSuccessCode.COMMENT_CREATE_SUCCESS,
                    commentGrpcClient.createComment(grpcRequest));
        } catch (StatusRuntimeException e) {
            log.error("Comment create failed via gRPC: {}", e.getStatus(), e);
            throw new RestApiException(QuestionErrorCode.CREATE_COMMENT_FAIL, getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Comment create failed", e);
            throw new RestApiException(QuestionErrorCode.CREATE_COMMENT_FAIL);
        }
    }

    @DeleteMapping("/{commentId}")
    public SuccessResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam String commentType,
            @LoginUser Long userId) {
        try {
            log.info("Comment delete request received for commentId: {}, commentType: {}",
                    commentId, commentType);

            DeleteCommentRequest request = DeleteCommentRequest.newBuilder()
                    .setCommentId(commentId)
                    .setUserId(userId)
                    .setCommentType(commentType)
                    .build();

            commentGrpcClient.deleteComment(request);
            return SuccessResponse.of(QuestionSuccessCode.COMMENT_DELETE_SUCCESS, null);
        } catch (StatusRuntimeException e) {
            log.error("Comment delete failed via gRPC: {}", e.getStatus(), e);
            throw new RestApiException(QuestionErrorCode.DELETE_COMMENT_FAIL, getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Comment delete failed", e);
            throw new RestApiException(QuestionErrorCode.DELETE_COMMENT_FAIL);
        }
    }

    @GetMapping("/{targetId}")
    public SuccessResponse<GetCommentResponseDto> getComment(
            @PathVariable Long targetId,
            @RequestParam String targetType,
            @RequestParam Integer pageNum,
            @LoginUser Long userId) {
        try {
            log.info("Comment get request received for targetId: {}, targetType: {}",
                    targetId, targetType);

            GetCommentRequest request = GetCommentRequest.newBuilder()
                    .setTargetId(targetId)
                    .setTargetType(targetType)
                    .setUserId(userId)
                    .setPageNum(pageNum-1)
                    .build();

            return SuccessResponse.of(QuestionSuccessCode.COMMENT_GET_SUCCESS,
                    commentGrpcClient.getComment(request));
        } catch (StatusRuntimeException e) {
            log.error("Comment get failed via gRPC: {}", e.getStatus(), e);
            throw new RestApiException(QuestionErrorCode.GET_COMMENT_FAIL, getGrpcErrorMessage(e));
        } catch (Exception e) {
            log.error("Comment get failed", e);
            throw new RestApiException(QuestionErrorCode.GET_COMMENT_FAIL);
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
