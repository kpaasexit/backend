package com.exit.gateway.controller.magazine.dto.response;

import com.exit.common.grpc.MagazineListItem;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

import static com.exit.common.util.time.TimeStampUtil.timestampToLocalDateTime;

@Builder
public record MagazineListItemDto(
        Long magazineId,
        Long magazineCategoryId,
        String magazineTitle,
        String magazineSubtitle,
        String magazineAuthor,
        String authorProfileUrl,
        String magazineThumbnailUrl,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static MagazineListItemDto from(MagazineListItem response) {
        return MagazineListItemDto.builder()
                .magazineId(response.getMagazineId())
                .magazineCategoryId(response.getMagazineCategoryId())
                .magazineTitle(response.getMagazineTitle())
                .magazineSubtitle(response.getMagazineSubtitle())
                .magazineAuthor(response.getMagazineAuthor())
                .authorProfileUrl(!response.getAuthorProfileUrl().isEmpty() ? response.getAuthorProfileUrl() : null)
                .magazineThumbnailUrl(!response.getMagazineThumbnailUrl().isEmpty() ? response.getMagazineThumbnailUrl() : null)
                .createdAt(timestampToLocalDateTime(response.getCreatedAt()))
                .build();
    }
}
