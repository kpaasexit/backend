package com.exit.gateway.controller.question;

import com.exit.common.grpc.CreateCommentRequest;
import com.exit.common.grpc.DeleteCommentRequest;
import com.exit.common.grpc.GetCommentRequest;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.QuestionSuccessCode;
import com.exit.gateway.controller.question.dto.request.comment.CreateCommentRequestDto;
import com.exit.gateway.controller.question.dto.response.comment.GetCommentResponseDto;
import com.exit.gateway.controller.question.dto.response.question.CreateCommentResponseDto;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.question.CommentGrpcClient;
import jakarta.validation.Valid;
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
            @LoginUser Long userId
    ) {
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
    }

    @DeleteMapping("/{commentId}")
    public SuccessResponse<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam String commentType,
            @LoginUser Long userId
    ) {
        log.info("Comment delete request received for commentId: {}, commentType: {}",
                commentId, commentType);

        DeleteCommentRequest request = DeleteCommentRequest.newBuilder()
                .setCommentId(commentId)
                .setUserId(userId)
                .setCommentType(commentType)
                .build();

        commentGrpcClient.deleteComment(request);
        return SuccessResponse.of(QuestionSuccessCode.COMMENT_DELETE_SUCCESS, null);
    }

    @GetMapping("/{targetId}")
    public SuccessResponse<GetCommentResponseDto> getCommentList(
            @PathVariable Long targetId,
            @RequestParam String targetType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer size,
            @LoginUser Long userId
    ) {
        log.info("Comment get request received for targetId: {}, targetType: {}",
                targetId, targetType);

        GetCommentRequest request = GetCommentRequest.newBuilder()
                .setTargetId(targetId)
                .setTargetType(targetType)
                .setUserId(userId)
                .setSize(size)
                .setPageNum(pageNum - 1)
                .build();

        return SuccessResponse.of(QuestionSuccessCode.COMMENT_GET_SUCCESS,
                commentGrpcClient.getComment(request));
    }
}
