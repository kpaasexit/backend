package com.exit.question.controller.dto.response;

import com.exit.question.domain.response.Response;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ResponseDetailDto(
        Long responseId,
        Long responseWriterId,
        String responseWriterName,
        String responseContent,
        Boolean responseAdopt,
        List<String> urls,
        Integer likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResponseDetailDto from(Response response, List<String> urls, Integer likeCount, String writerName) {
        ResponseDetailDtoBuilder builder = ResponseDetailDto.builder();

        if (urls != null && !urls.isEmpty()) {
            builder.urls(urls);
        }

        return builder
                .responseId(response.getResponseId())
                .responseWriterId(response.getResponseWriterId())
                .responseWriterName(writerName)
                .responseContent(response.getResponseContent())
                .responseAdopt(response.getResponseAdopt())
                .likeCount(likeCount)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}
