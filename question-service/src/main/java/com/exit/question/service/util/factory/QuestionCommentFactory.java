package com.exit.question.service.util.factory;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.domain.Comment;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionComment;
import com.exit.question.domain.question.repository.QuestionCommentRepository;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.exception.GrpcCommentErrorCode;
import com.exit.question.exception.GrpcQuestionErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCommentFactory extends CommentFactory {
    private final QuestionRepository questionRepository;
    private final QuestionCommentRepository questionCommentRepository;

    @Override
    public Comment createAndSaveComment(Long targetId, Long authorId, String content) {
        Question question = questionRepository.findById(targetId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        QuestionComment comment = QuestionComment.builder()
                .question(question)
                .writerId(authorId)
                .content(content)
                .build();

        return questionCommentRepository.save(comment);
    }

    @Override
    public void deleteComment(Long targetId, Long authorId) {
        validateCommentWriter(targetId, authorId);
        questionCommentRepository.deleteById(targetId);
    }

    @Override
    public SendNotificationRequest createSendNotificationRequest(Long targetId, String deviceId) {
        NotificationContentDto dto = questionRepository.findContentById(targetId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NULL_QUESTION));

        return SendNotificationRequest.newBuilder()
                .setBody(dto.content())
                .setType("NEW_COMMENT")
                .setTargetId(targetId)
                .setReceiverId(dto.receiverId())
                .setDeviceId(deviceId)
                .build();
    }

    private void validateCommentWriter(Long commentId, Long userId) {
        boolean isAuthorized = questionCommentRepository.existsByIdAndWriterId(commentId, userId);
        if (!isAuthorized) {
            throw new GrpcException(GrpcCommentErrorCode.COMMENT_WRITER_MISMATCH);
        }
    }
}
