package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.ResponseDetail;
import com.exit.common.util.time.TimeStampUtil;
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
    public static ResponseDetailDto from(ResponseDetail grpcResponse) {
        return ResponseDetailDto.builder()
                .responseId(grpcResponse.getResponseId())
                .responseWriterId(grpcResponse.getResponseWriterId())
                .responseWriterName(grpcResponse.getResponseWriterName())
                .responseContent(grpcResponse.getResponseContent())
                .responseAdopt(grpcResponse.getResponseAdopt())
                .urls(grpcResponse.getUrlsList())
                .likeCount(grpcResponse.getLikeCount())
                .createdAt(TimeStampUtil.timestampToLocalDateTime(grpcResponse.getCreatedAt()))
                .updatedAt(TimeStampUtil.timestampToLocalDateTime(grpcResponse.getUpdatedAt()))
                .build();
    }
}