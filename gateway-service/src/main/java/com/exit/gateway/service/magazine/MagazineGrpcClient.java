package com.exit.gateway.service.magazine;

import com.exit.common.grpc.*;
import com.exit.gateway.controller.magazine.dto.response.*;
import com.exit.gateway.global.annotation.GrpcToRest;
import com.exit.gateway.global.util.mapper.error.magazine.MagazineGrpcErrorMapper;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@GrpcToRest(mapper = MagazineGrpcErrorMapper.class)
public class MagazineGrpcClient {
    @GrpcClient("magazine-service")
    private MagazineServiceGrpc.MagazineServiceBlockingStub magazineServiceStub;

    public MagazineItemListDto getMagazinesByCategory(GetMagazinesByCategoryRequest request) {
        log.debug("Sending getMagazinesByCategory request via gRPC: categoryId={}, pageNum={}",
                request.getCategoryId(), request.getPageNum());
        GetMagazinesByCategoryResponse response = magazineServiceStub.getMagazinesByCategory(request);
        log.debug("Received getMagazinesByCategory response via gRPC: {} magazines found", response.getMagazineItemCount());

        return MagazineItemListDto.from(response);
    }

    public MagazineItemDto getMagazine(GetMagazineRequest request) {
        log.debug("Sending getMagazine request via gRPC: magazineId={}", request.getMagazineId());
        GetMagazineResponse response = magazineServiceStub.getMagazine(request);
        log.debug("Received getMagazine response via gRPC: magazineId={}", response.getMagazineItem().getMagazineId());

        return MagazineItemDto.from(response.getMagazineItem());
    }

    public ScrapMagazineResponseDto scrapMagazine(ScrapMagazineRequest request) {
        log.debug("Sending scrapMagazine request via gRPC: magazineId={}", request.getMagazineId());
        ScrapMagazineResponse response = magazineServiceStub.scrapMagazine(request);
        log.debug("Received getMagazine response via gRPC: magazineId={}", response.getMagazineId());

        return ScrapMagazineResponseDto.from(response);
    }

    public GetScrapBoxResponseDto getScrapBox(GetScrapBoxRequest request) {
        log.debug("Sending getScrapBox request via gRPC: userId={}", request.getUserId());
        GetScrapBoxResponse response = magazineServiceStub.getScrapBox(request);
        log.debug("Received getScrapBox response via gRPC");

        return GetScrapBoxResponseDto.from(response);
    }

    public GetRecommendedMagazineResponseDto getRecommendedMagazine(GetRecommendedMagazineRequest request) {
        log.debug("Sending getRecommendedMagazine request via gRPC: userId={}", request.getUserId());
        GetRecommendedMagazineResponse response = magazineServiceStub.getRecommendedMagazine(request);
        log.debug("Received getRecommendedMagazine response via gRPC");

        return GetRecommendedMagazineResponseDto.from(response);
    }

    public SearchMagazinesResponse searchMagazines(String keyword, int page, int size) {
        // 빈 문자열이나 공백만 있는 경우 빈 문자열로 정규화
        String normalizedKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();
        log.debug("Sending search magazines request via gRPC for keyword: '{}'", normalizedKeyword);

        SearchMagazinesRequest request = SearchMagazinesRequest.newBuilder()
                .setKeyword(normalizedKeyword)
                .setPage(page)
                .setSize(size)
                .build();

        SearchMagazinesResponse response = magazineServiceStub.searchMagazines(request);
        log.debug("Received search magazines response via gRPC with {} results", response.getMagazinesCount());
        return response;
    }
}