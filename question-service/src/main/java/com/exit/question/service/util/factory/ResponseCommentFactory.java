package com.exit.question.service.util.factory;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.domain.Comment;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseComment;
import com.exit.question.domain.response.repository.ResponseCommentRepository;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcCommentErrorCode;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.exception.GrpcResponseErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ResponseCommentFactory extends CommentFactory {
    private final ResponseRepository responseRepository;
    private final ResponseCommentRepository responseCommentRepository;

    @Override
    public Comment createAndSaveComment(Long targetId, Long authorId, String content) {
        Response response = responseRepository.findById(targetId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        ResponseComment comment = ResponseComment.builder()
                .response(response)
                .writerId(authorId)
                .content(content)
                .build();

        return responseCommentRepository.save(comment);
    }

    @Override
    public void deleteComment(Long targetId, Long authorId) {
        validateCommentWriter(targetId, authorId);
        responseCommentRepository.deleteById(targetId);
    }

    @Override
    public SendNotificationRequest createSendNotificationRequest(Long targetId, String deviceId) {

        NotificationContentDto dto = responseRepository.findContentById(targetId)
                .orElseThrow(() -> new GrpcException(GrpcResponseErrorCode.NULL_RESPONSE));
        return SendNotificationRequest.newBuilder()
                .setBody(dto.content())
                .setType("NEW_COMMENT")
                .setTargetId(targetId)
                .setReceiverId(dto.receiverId())
                .setDeviceId(deviceId)
                .build();
    }

    private void validateCommentWriter(Long commentId, Long userId) {
        boolean isAuthorized = responseCommentRepository.existsByIdAndWriterId(commentId, userId);
        if (!isAuthorized) {
            throw new GrpcException(GrpcCommentErrorCode.COMMENT_WRITER_MISMATCH);
        }
    }
}
