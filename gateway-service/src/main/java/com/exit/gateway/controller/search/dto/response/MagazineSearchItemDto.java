package com.exit.gateway.controller.search.dto.response;

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
}
