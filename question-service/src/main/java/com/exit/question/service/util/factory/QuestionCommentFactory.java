package com.exit.question.service.util.factory;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.Authority;
import com.exit.common.grpc.CommentItem;
import com.exit.common.grpc.GetCommentRequest;
import com.exit.common.grpc.GetCommentResponse;
import com.exit.common.grpc.GetUsersNameAndProfileResponse;
import com.exit.common.grpc.SendNotificationRequest;
import com.exit.common.grpc.UpdateAdditionalUserInfoResponse;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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
    public GetCommentResponse getCommentList(GetCommentRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPageNum(), request.getSize());
        Page<QuestionComment> questionComments = questionCommentRepository.findAllByQuestion_QuestionId(
                request.getTargetId(), pageRequest);

        Set<Long> commentAuthorIds = getCommentAuthorIds(questionComments.getContent());
        GetUsersNameAndProfileResponse usersNameAndProfile = userGrpcClient.getUsersNameAndProfile(commentAuthorIds);

        Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = getUserInfoMap(usersNameAndProfile);

        List<CommentItem> commentItemList = createCommentItemList(questionComments.getContent(), userInfoMap,
                request.getUserId());
        return GetCommentResponse.newBuilder()
                .addAllComment(commentItemList)
                .setHasNext(questionComments.hasNext())
                .setCurrentPage(questionComments.getNumber() + 1)
                .setTotalPageNum(questionComments.getTotalPages())
                .build();
    }

    private List<CommentItem> createCommentItemList(List<QuestionComment> questionComments,
                                                    Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap,
                                                    Long userId) {
        return questionComments.stream()
                .map(comment -> {
                    Authority authority = getCommentAuthority(comment, userId);

                    return CommentItem.newBuilder()
                            .setCommentId(comment.getCommentId())
                            .setContent(comment.getContent())
                            .setCreatedAt(TimeStampUtil.toGrpcTimestamp(comment.getCreatedAt()))
                            .setNickname(userInfoMap.get(comment.getAuthorId()).getUserName())
                            .setProfileImage(userInfoMap.get(comment.getAuthorId()).getUserProfile())
                            .setAuthority(authority)
                            .build();
                })
                .toList();
    }

    private Authority getCommentAuthority(QuestionComment comment, Long userId) {
        boolean isSameUser = Objects.equals(comment.getAuthorId(), userId);
        return Authority.newBuilder()
                .setCanModify(isSameUser)
                .setCanDelete(isSameUser)
                .build();
    }

    private Map<Long, UpdateAdditionalUserInfoResponse> getUserInfoMap(
            GetUsersNameAndProfileResponse usersNameAndProfile) {
        return usersNameAndProfile.getUserInfoList().stream()
                .collect(Collectors.toMap(UpdateAdditionalUserInfoResponse::getUserId,
                        Function.identity(),
                        (existing, replacement) -> replacement)
                );
    }

    private Set<Long> getCommentAuthorIds(List<QuestionComment> questionComments) {
        return questionComments.stream()
                .map(QuestionComment::getAuthorId)
                .collect(Collectors.toSet());
    }

    private void validateCommentWriter(Long commentId, Long userId) {
        boolean isAuthorized = questionCommentRepository.existsByIdAndWriterId(commentId, userId);
        if (!isAuthorized) {
            throw new GrpcException(GrpcCommentErrorCode.COMMENT_WRITER_MISMATCH);
        }
    }
}
