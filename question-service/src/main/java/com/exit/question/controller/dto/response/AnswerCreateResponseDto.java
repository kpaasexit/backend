package com.exit.question.controller.dto.response;

import com.exit.question.domain.response.Response;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AnswerCreateResponseDto(
        Long responseId,
        Long questionId,
        Long responseWriterId,
        String responseContent,
        Boolean responseAdopt,
        List<String> imageUrls,
        Integer likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnswerCreateResponseDto from(Response answer, List<String> urls) {
        AnswerCreateResponseDtoBuilder builder = AnswerCreateResponseDto.builder();
        if(urls != null && !urls.isEmpty()){
            builder.imageUrls(urls);
        }

        return builder.responseId(answer.getResponseId())
                    .questionId(answer.getQuestionId())
                    .responseWriterId(answer.getResponseWriterId())
                    .responseContent(answer.getResponseContent())
                    .responseAdopt(answer.getResponseAdopt())
                    .likeCount(0)
                    .createdAt(answer.getCreatedAt())
                    .updatedAt(answer.getUpdatedAt())
                    .build();
    }
}
