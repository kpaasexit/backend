package com.exit.gateway.controller.question.dto.response.comment;

import com.exit.common.grpc.GetCommentResponse;
import com.exit.common.util.time.TimeStampUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record GetCommentResponseDto(
        List<CommentItem> commentItemList,
        Boolean hasNext
) {
    public static GetCommentResponseDto from(GetCommentResponse response) {
        List<CommentItem> commentItems = response.getCommentList().stream()
                .map(comment -> CommentItem.builder()
                        .commentId(comment.getCommentId())
                        .content(comment.getContent())
                        .nickname(comment.getNickname())
                        .profileImage(comment.getProfileImage())
                        .createdAt(TimeStampUtil.timestampToLocalDateTime(comment.getCreatedAt()))
                        .build()
                ).toList();

        return GetCommentResponseDto.builder()
                .commentItemList(commentItems)
                .hasNext(response.getHasNext())
                .build();
    }

    @Builder
    public record CommentItem(
            Long commentId,
            String content,
            String nickname,
            String profileImage,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {
    }
}