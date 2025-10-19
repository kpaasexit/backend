package com.exit.gateway.controller.magazine.dto.response;

import com.exit.common.grpc.MagazineItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record MagazineItemDto(
        Long magazineId,
        Long magazineCategoryId,
        String magazineTitle,
        String magazineSubtitle,
        String magazineContent,
        String magazineAuthor,
        String authorProfileUrl,
        String magazineThumbnailUrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        Boolean isScrap
) {
    public static MagazineItemDto from(MagazineItem response) {
        return MagazineItemDto.builder()
                .magazineId(response.getMagazineId())
                .magazineCategoryId(response.getMagazineCategoryId())
                .magazineTitle(response.getMagazineTitle())
                .magazineSubtitle(response.getMagazineSubtitle())
                .magazineContent(response.getMagazineContent())
                .magazineAuthor(response.getMagazineAuthor())
                .authorProfileUrl(!response.getAuthorProfileUrl().isEmpty() ? response.getAuthorProfileUrl() : null)
                .magazineThumbnailUrl(!response.getMagazineThumbnailUrl().isEmpty() ? response.getMagazineThumbnailUrl() : null)
                .createdAt(timestampToLocalDateTime(response.getCreatedAt()))
                .isScrap(response.getIsScrap())
                .build();

    }
}
