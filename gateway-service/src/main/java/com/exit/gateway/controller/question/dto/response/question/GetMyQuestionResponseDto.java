package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.GetMyQuestionResponse;
import com.exit.common.util.time.TimeStampUtil;
import lombok.Builder;

import java.util.List;

@Builder
public record GetMyQuestionResponseDto(
        List<GetPopularPostResponseDto.PopularPost> questions,
        Boolean hasNext,
        Integer currentPage,
        Integer totalPageNum
) {
    public static GetMyQuestionResponseDto from(GetMyQuestionResponse response) {
        List<GetPopularPostResponseDto.PopularPost> posts = response.getPostList().stream()
                .map(post -> {
                    GetPopularPostResponseDto.PopularPost.PopularPostBuilder builder = GetPopularPostResponseDto.PopularPost.builder();

                    if (!post.getProfileUrl().isEmpty()) builder.profileUrl(post.getProfileUrl());

                    return builder
                            .categoryId(post.getCategoryId())
                            .nickname(post.getNickname())
                            .createdAt(TimeStampUtil.timestampToLocalDateTime(post.getCreatedAt()))
                            .title(post.getTitle())
                            .content(post.getContent())
                            .isAnswered(post.getIsAnswered())
                            .answerCount(post.getAnswerCount())
                            .build();
                }).toList();

        return GetMyQuestionResponseDto.builder()
                .questions(posts)
                .hasNext(response.getHasNext())
                .currentPage(response.getCurrentPage())
                .totalPageNum(response.getTotalPageNum())
                .build();
    }
}
