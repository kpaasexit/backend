package com.exit.question.service.util.factory;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.domain.Comment;
import com.exit.question.domain.question.Question;
import com.exit.question.domain.question.QuestionComment;
import com.exit.question.domain.question.repository.QuestionCommentRepository;
import com.exit.question.domain.question.repository.QuestionRepository;
import com.exit.question.exception.GrpcCommentErrorCode;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.service.client.UserGrpcClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCommentFactory extends CommentFactory {
    private final QuestionRepository questionRepository;
    private final QuestionCommentRepository questionCommentRepository;
    private final UserGrpcClient userGrpcClient;

    @Override
    public Comment createAndSaveComment(Long targetId, Long authorId, String content) {
        Question question = questionRepository.findById(targetId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

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
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

        return SendNotificationRequest.newBuilder()
                .setBody(dto.content())
                .setType("NEW_COMMENT")
                .setTargetId(targetId)
                .setReceiverId(dto.receiverId())
                .build();
    }

    @Override
    public GetCommentResponse getCommentList(Long targetId, Long userId, Integer pageNum) {
        PageRequest pageRequest = PageRequest.of(pageNum, 5);
        Slice<QuestionComment> questionComments = questionCommentRepository.findAllByQuestion_QuestionId(targetId, pageRequest);

        List<Long> commentAuthorIds = getCommentAuthorIds(questionComments.getContent());
        GetUsersNameAndProfileResponse usersNameAndProfile = userGrpcClient.getUsersNameAndProfile(commentAuthorIds);

        Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = getUserInfoMap(usersNameAndProfile);

        List<CommentItem> commentItemList = createCommentItemList(questionComments.getContent(), userInfoMap);
        return GetCommentResponse.newBuilder()
                .addAllComment(commentItemList)
                .setHasNext(questionComments.hasNext())
                .build();
    }

    private List<CommentItem> createCommentItemList(List<QuestionComment> questionComments, Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap) {
        return questionComments.stream()
                .map(comment -> {
                    return CommentItem.newBuilder()
                            .setCommentId(comment.getCommentId())
                            .setContent(comment.getContent())
                            .setCreatedAt(TimeStampUtil.toGrpcTimestamp(comment.getCreatedAt()))
                            .setNickname(userInfoMap.get(comment.getAuthorId()).getUserName())
                            .setProfileImage(userInfoMap.get(comment.getAuthorId()).getUserProfile())
                            .build();
                })
                .toList();
    }

    private Map<Long, UpdateAdditionalUserInfoResponse> getUserInfoMap(GetUsersNameAndProfileResponse usersNameAndProfile) {
        return usersNameAndProfile.getUserInfoList().stream()
                .collect(Collectors.toMap(UpdateAdditionalUserInfoResponse::getUserId,
                        Function.identity(),
                        (existing, replacement) -> replacement)
                );
    }

    private List<Long> getCommentAuthorIds(List<QuestionComment> questionComments) {
        return questionComments.stream()
                .map(QuestionComment::getAuthorId)
                .toList();
    }

    private void validateCommentWriter(Long commentId, Long userId) {
        boolean isAuthorized = questionCommentRepository.existsByIdAndWriterId(commentId, userId);
        if (!isAuthorized) {
            throw new GrpcException(GrpcCommentErrorCode.COMMENT_WRITER_MISMATCH);
        }
    }
}
