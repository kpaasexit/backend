package com.exit.gateway.controller.magazine.dto.response;

import com.exit.common.grpc.GetMagazinesByCategoryResponse;

import java.util.List;

public record MagazineItemListDto(
        List<MagazineItemDto> magazineItems
) {
    public static MagazineItemListDto from(GetMagazinesByCategoryResponse response) {
        List<MagazineItemDto> list = response.getMagazineItemList()
                .stream()
                .map(MagazineItemDto::from)
                .toList();
        return new MagazineItemListDto(list);
    }
}