package com.exit.gateway.controller.magazine;

import com.exit.common.grpc.*;
import com.exit.common.response.SuccessResponse;
import com.exit.common.response.success.MagazineSuccessCode;
import com.exit.gateway.controller.magazine.dto.response.*;
import com.exit.gateway.global.annotation.LoginUser;
import com.exit.gateway.service.magazine.MagazineGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.Integers;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/magazine")
@RequiredArgsConstructor
@Slf4j
public class MagazineController {
    private final MagazineGrpcClient magazineGrpcClient;

    @GetMapping("/category/{categoryId}")
    public SuccessResponse<MagazineListDto> getMagazinesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer size
    ) {
        log.info("Get magazines by category request received: categoryId={}, pageNum={}", categoryId, pageNum - 1);
        GetMagazinesByCategoryRequest request = GetMagazinesByCategoryRequest.newBuilder()
                .setCategoryId(categoryId)
                .setPageNum(pageNum - 1)
                .setSize(size)
                .build();

        return SuccessResponse.of(MagazineSuccessCode.GET_MAGAZINE_LIST_BY_CATEGORY_SUCCESS,
                magazineGrpcClient.getMagazinesByCategory(request));
    }

    @GetMapping("/{magazineId}")
    public SuccessResponse<MagazineItemDto> getMagazine(
            @PathVariable Long magazineId,
            @LoginUser Long userId) {
        log.info("Get magazine request received: magazineId={}", magazineId);
        GetMagazineRequest request = GetMagazineRequest.newBuilder()
                .setMagazineId(magazineId)
                .setUserId(userId)
                .build();

        return SuccessResponse.of(MagazineSuccessCode.GET_MAGAZINE_SUCCESS,
                magazineGrpcClient.getMagazine(request));
    }

    @PostMapping("/{magazineId}/scrap")
    public SuccessResponse<ScrapMagazineResponseDto> scrapMagazine(
            @PathVariable Long magazineId,
            @LoginUser Long userId) {
        log.info("Scrap magazine request received: magazineId={}", magazineId);
        ScrapMagazineRequest request = ScrapMagazineRequest.newBuilder()
                .setMagazineId(magazineId)
                .setUserId(userId)
                .build();

        return SuccessResponse.of(MagazineSuccessCode.SCRAP_MAGAZINE_SUCCESS,
                magazineGrpcClient.scrapMagazine(request));
    }

    @GetMapping("/scrap-box")
    public SuccessResponse<GetScrapBoxResponseDto> getScrapBox(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer size
            ) {
        log.info("Get scrap-box request received");
        GetScrapBoxRequest request = GetScrapBoxRequest.newBuilder()
                .setUserId(userId)
                .setPageNum(pageNum - 1)
                .setSize(size)
                .build();

        return SuccessResponse.of(MagazineSuccessCode.GET_SCRAP_BOX_SUCCESS,
                magazineGrpcClient.getScrapBox(request));
    }

    @GetMapping("/recommend")
    public SuccessResponse<GetRecommendedMagazineResponseDto> getRecommendMagazine(
            @LoginUser Long userId
    ) {
        log.info("Get recomment magazine request received");
        GetRecommendedMagazineRequest request = GetRecommendedMagazineRequest.newBuilder()
                .setUserId(userId)
                .build();

        return SuccessResponse.of(MagazineSuccessCode.GET_RECOMMENDED_MAGAZINE_SUCCESS,
                magazineGrpcClient.getRecommendedMagazine(request));
    }
}
