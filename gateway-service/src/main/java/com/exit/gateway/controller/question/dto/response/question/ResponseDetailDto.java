package com.exit.gateway.controller.question.dto.response.question;

import com.exit.common.grpc.ResponseDetail;
import com.exit.common.util.time.TimeStampUtil;
import com.exit.gateway.controller.question.dto.ImageObjectDto;
import com.exit.gateway.controller.question.dto.response.authority.ResponseAuthority;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ResponseDetailDto(
        Long responseId,
        Long responseWriterId,
        String responseWriterName,
        String responseWriterProfile,
        String responseContent,
        Boolean responseAdopt,
        List<ImageObjectDto> images,
        Integer likeCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,
        Boolean isAi,
        ResponseAuthority authority
) {
    public static ResponseDetailDto from(ResponseDetail grpcResponse) {
        ResponseDetailDtoBuilder builder = ResponseDetailDto.builder();
        if (!grpcResponse.getProfile().isEmpty())
            builder.responseWriterProfile(grpcResponse.getProfile());

        if (!grpcResponse.getImageList().isEmpty()) {
            List<ImageObjectDto> images = grpcResponse.getImageList().stream().map(
                    imageObject -> {
                        return ImageObjectDto.builder()
                                .imageId(imageObject.getImageId())
                                .imageUrl(imageObject.getImageUrl())
                                .build();
                    }
            ).toList();
            builder.images(images);
        }

        return builder
                .responseId(grpcResponse.getResponseId())
                .responseWriterId(grpcResponse.getResponseWriterId())
                .responseWriterName(grpcResponse.getResponseWriterName())
                .responseContent(grpcResponse.getResponseContent())
                .responseAdopt(grpcResponse.getResponseAdopt())
                .likeCount(grpcResponse.getLikeCount())
                .createdAt(TimeStampUtil.timestampToLocalDateTime(grpcResponse.getCreatedAt()))
                .updatedAt(TimeStampUtil.timestampToLocalDateTime(grpcResponse.getUpdatedAt()))
                .isAi(grpcResponse.getIsAi())
                .authority(ResponseAuthority.from(grpcResponse.getAuthority()))
                .build();
    }
}