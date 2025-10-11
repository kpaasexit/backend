package com.exit.gateway.controller.magazine.dto.response;

import com.exit.common.grpc.GetScrapBoxResponse;
import com.exit.common.util.time.TimeStampUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record GetScrapBoxResponseDto(
        List<MagazineScrapBoxItem> scrapBoxItems,
        Boolean hasNext
) {
    public static GetScrapBoxResponseDto from(GetScrapBoxResponse response) {
        List<MagazineScrapBoxItem> list = response.getMagazineScrapBoxItemList().stream()
                .map(item -> MagazineScrapBoxItem.builder()
                        .magazineId(item.getMagazineId())
                        .magazineTitle(item.getMagazineTitle())
                        .magazineSubtitle(item.getMagazineSubtitle())
                        .magazineThumbnailUrl(item.getMagazineThumbnailUrl())
                        .createdAt(timestampToLocalDateTime(item.getCreatedAt()))
                        .build()
                ).toList();

        return GetScrapBoxResponseDto.builder()
                .scrapBoxItems(list)
                .hasNext(response.getHasNext())
                .build();
    }

    @Builder
    public record MagazineScrapBoxItem(
            Long magazineId,
            String magazineTitle,
            String magazineSubtitle,
            String magazineThumbnailUrl,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime createdAt
    ) {
    }
}
