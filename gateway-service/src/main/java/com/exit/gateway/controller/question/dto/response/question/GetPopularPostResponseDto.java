package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.GetPopularPostResponse;
import com.exit.common.util.time.TimeStampUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record GetPopularPostResponseDto(
        List<PopularPost> popularPostList
) {
    public static GetPopularPostResponseDto from(GetPopularPostResponse response) {
        List<PopularPost> posts = response.getPostList().stream()
                .map(post -> {
                    PopularPost.PopularPostBuilder builder = PopularPost.builder();

                    if (!post.getProfileUrl().isEmpty()) builder.profileUrl(post.getProfileUrl());

                    return builder
                            .questionId(post.getQuestionId())
                            .categoryId(post.getCategoryId())
                            .nickname(post.getNickname())
                            .createdAt(TimeStampUtil.timestampToLocalDateTime(post.getCreatedAt()))
                            .title(post.getTitle())
                            .content(post.getContent())
                            .isAnswered(post.getIsAnswered())
                            .answerCount(post.getAnswerCount())
                            .build();
                }).toList();

        return GetPopularPostResponseDto.builder()
                .popularPostList(posts)
                .build();
    }

    @Builder
    public record PopularPost(
            Long questionId,
            Long categoryId,
            String profileUrl,
            String nickname,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt,
            String title,
            String content,
            Boolean isAnswered,
            Integer answerCount
    ) {
    }
}