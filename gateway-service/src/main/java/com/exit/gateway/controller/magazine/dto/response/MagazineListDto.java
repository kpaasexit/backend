package com.exit.gateway.controller.magazine.dto.response;

import com.exit.common.grpc.GetMagazinesByCategoryResponse;

import com.exit.common.grpc.SearchMagazinesResponse;
import java.util.List;

public record MagazineListDto(
        List<MagazineListItemDto> magazineListItems,
        Integer currentPage,
        Boolean hasNext
) {
    public static MagazineListDto from(GetMagazinesByCategoryResponse response) {
        List<MagazineListItemDto> list = response.getMagazineItemList()
                .stream()
                .map(MagazineListItemDto::from)
                .toList();
        return new MagazineListDto(list, response.getCurrentPage(), response.getHasNext());
    }

    public static MagazineListDto from(SearchMagazinesResponse response) {
        List<MagazineListItemDto> magazines = response.getMagazinesList().stream()
                .map(MagazineListItemDto::from)
                .toList();
        return new MagazineListDto(magazines, response.getCurrentPage(), response.getHasNext());
    }
}