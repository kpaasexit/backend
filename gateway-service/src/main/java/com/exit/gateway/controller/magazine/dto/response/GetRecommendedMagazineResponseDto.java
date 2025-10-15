package com.exit.gateway.controller.magazine.dto.response;

import com.exit.common.grpc.GetRecommendedMagazineResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record GetRecommendedMagazineResponseDto(
        List<RecommendedMagazineItem> recommendedMagazineItems
) {
    public static GetRecommendedMagazineResponseDto from(GetRecommendedMagazineResponse response) {
        List<RecommendedMagazineItem> list = response.getRecommendMagazineList().stream()
                .map(item -> RecommendedMagazineItem.builder()
                        .magazineId(item.getMagazineId())
                        .magazineTitle(item.getMagazineTitle())
                        .magazineSubtitle(item.getMagazineSubtitle())
                        .magazineThumbnailUrl(item.getMagazineThumbnailUrl())
                        .createdAt(timestampToLocalDateTime(item.getCreatedAt()))
                        .build()
                ).toList();

        return GetRecommendedMagazineResponseDto.builder()
                .recommendedMagazineItems(list)
                .build();
    }

    @Builder
    public record RecommendedMagazineItem(
            Long magazineId,
            String magazineTitle,
            String magazineSubtitle,
            String magazineThumbnailUrl,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {
    }
}
