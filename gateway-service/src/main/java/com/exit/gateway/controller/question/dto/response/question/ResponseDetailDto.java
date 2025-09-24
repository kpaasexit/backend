package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.ResponseDetail;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public static ResponseDetailDto from(ResponseDetail grpcResponse) {
        return ResponseDetailDto.builder()
                .responseId(grpcResponse.getResponseId())
                .responseWriterId(grpcResponse.getResponseWriterId())
                .responseWriterName(grpcResponse.getResponseWriterName())
                .responseContent(grpcResponse.getResponseContent())
                .responseAdopt(grpcResponse.getResponseAdopt())
                .urls(grpcResponse.getUrlsList())
                .likeCount(grpcResponse.getLikeCount())
                .createdAt(LocalDateTime.parse(grpcResponse.getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .updatedAt(LocalDateTime.parse(grpcResponse.getUpdatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}