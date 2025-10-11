package com.exit.gateway.controller.magazine.dto.response;

import com.exit.common.grpc.ScrapMagazineResponse;
import lombok.Builder;

@Builder
public record ScrapMagazineResponseDto(
        Long magazineId,
        Boolean isScrapped
) {
    public static ScrapMagazineResponseDto from(ScrapMagazineResponse response) {
        return ScrapMagazineResponseDto.builder()
                .magazineId(response.getMagazineId())
                .isScrapped(response.getIsScrapped())
                .build();
    }
}
