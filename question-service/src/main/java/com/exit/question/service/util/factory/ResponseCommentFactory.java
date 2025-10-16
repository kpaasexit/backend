package com.exit.question.service.util.factory;

import com.exit.common.exception.grpc.GrpcException;
import com.exit.common.grpc.*;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.question.controller.dto.request.NotificationContentDto;
import com.exit.question.domain.Comment;
import com.exit.question.domain.response.Response;
import com.exit.question.domain.response.ResponseComment;
import com.exit.question.domain.response.repository.ResponseCommentRepository;
import com.exit.question.domain.response.repository.ResponseRepository;
import com.exit.question.exception.GrpcCommentErrorCode;
import com.exit.question.exception.GrpcQuestionErrorCode;
import com.exit.question.exception.GrpcResponseErrorCode;
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
public class ResponseCommentFactory extends CommentFactory {
    private final ResponseRepository responseRepository;
    private final ResponseCommentRepository responseCommentRepository;
    private final UserGrpcClient userGrpcClient;

    @Override
    public Comment createAndSaveComment(Long targetId, Long authorId, String content) {
        Response response = responseRepository.findById(targetId)
                .orElseThrow(() -> new GrpcException(GrpcQuestionErrorCode.NOT_FOUND_QUESTION));

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
                .build();
    }

    @Override
    public GetCommentResponse getCommentList(Long targetId, Long userId, Integer pageNum) {
        PageRequest pageRequest = PageRequest.of(pageNum, 5);
        Slice<ResponseComment> responseComments = responseCommentRepository.findAllByResponse_ResponseId(targetId, pageRequest);

        List<Long> commentAuthorIds = getCommentAuthorIds(responseComments.getContent());
        GetUsersNameAndProfileResponse usersNameAndProfile = userGrpcClient.getUsersNameAndProfile(commentAuthorIds);

        Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap = getUserInfoMap(usersNameAndProfile);

        List<CommentItem> commentItemList = createCommentItemList(responseComments.getContent(), userInfoMap);
        return GetCommentResponse.newBuilder()
                .addAllComment(commentItemList)
                .setHasNext(responseComments.hasNext())
                .build();
    }

    private List<CommentItem> createCommentItemList(List<ResponseComment> responseComments, Map<Long, UpdateAdditionalUserInfoResponse> userInfoMap) {
        return responseComments.stream()
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

    private List<Long> getCommentAuthorIds(List<ResponseComment> responseComment) {
        return responseComment.stream()
                .map(ResponseComment::getAuthorId)
                .toList();
    }

    private void validateCommentWriter(Long commentId, Long userId) {
        boolean isAuthorized = responseCommentRepository.existsByIdAndWriterId(commentId, userId);
        if (!isAuthorized) {
            throw new GrpcException(GrpcCommentErrorCode.COMMENT_WRITER_MISMATCH);
        }
    }
}
