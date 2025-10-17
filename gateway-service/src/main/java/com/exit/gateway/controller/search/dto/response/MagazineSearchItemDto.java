package com.exit.gateway.controller.search.dto.response;

import com.exit.common.grpc.MagazineItem;
import com.exit.common.util.time.TimeStampUtil;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MagazineSearchItemDto(
        Long magazineId,
        Long magazineCategoryId,
        String magazineTitle,
        String magazineSubtitle,
        String magazineContent,
        String magazineAuthor,
        String authorProfileUrl,
        String magazineThumbnailUrl,
        LocalDateTime createdAt
) {
    public static MagazineSearchItemDto toMagazineDto(MagazineItem item) {
        MagazineSearchItemDtoBuilder builder = MagazineSearchItemDto.builder();
        if (!item.getAuthorProfileUrl().isEmpty()) {
            builder.authorProfileUrl(item.getAuthorProfileUrl());
        }
        return builder
                .magazineId(item.getMagazineId())
                .magazineCategoryId(item.getMagazineCategoryId())
                .magazineTitle(item.getMagazineTitle())
                .magazineSubtitle(item.getMagazineSubtitle())
                .magazineContent(item.getMagazineContent())
                .magazineAuthor(item.getMagazineAuthor())
                .magazineThumbnailUrl(item.getMagazineThumbnailUrl())
                .createdAt(TimeStampUtil.timestampToLocalDateTime(item.getCreatedAt()))
                .build();
    }
}
