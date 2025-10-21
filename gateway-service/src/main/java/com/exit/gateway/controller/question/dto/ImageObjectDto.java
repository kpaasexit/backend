package com.exit.gateway.controller.question.dto;

import lombok.Builder;

@Builder
public record ImageObjectDto(
        Long imageId,
        String imageUrl
) {
}
